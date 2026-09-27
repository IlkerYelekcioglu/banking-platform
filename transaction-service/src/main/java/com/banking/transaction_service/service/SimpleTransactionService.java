package com.banking.transaction_service.service;

import com.banking.transaction_service.client.AccountClient;
import com.banking.transaction_service.dto.request.BalanceOperationRequest;
import com.banking.transaction_service.dto.request.TransactionCreateRequest;
import com.banking.transaction_service.dto.response.AccountResponse;
import com.banking.transaction_service.dto.response.TransactionResponse;
import com.banking.transaction_service.entity.OutboxEvent;
import com.banking.transaction_service.entity.Transaction;
import com.banking.transaction_service.enums.FraudDecision;
import com.banking.transaction_service.enums.TransactionStatus;
import com.banking.transaction_service.event.FraudDecisionEvent;
import com.banking.transaction_service.event.TransactionCompletedEvent;
import com.banking.transaction_service.event.TransactionRequestedEvent;
import com.banking.transaction_service.exception.InsufficientBalanceException;
import com.banking.transaction_service.exception.TransactionNotFoundException;
import com.banking.transaction_service.exception.InvalidTransactionException;
import com.banking.transaction_service.exception.TransactionProcessingException;
import com.banking.transaction_service.mapper.TransactionMapper;
import com.banking.transaction_service.repository.OutboxEventRepository;
import com.banking.transaction_service.repository.TransactionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class SimpleTransactionService
    implements TransactionService {
  private final TransactionRepository transactionRepository;
  private final TransactionMapper transactionMapper;
  private final AccountClient accountClient;

  private final OutboxEventRepository outboxEventRepository;

  private final ObjectMapper objectMapper;

  @Override
  @Transactional
  public TransactionResponse createTransaction(
      TransactionCreateRequest request) {

    Transaction existingTransaction =
        transactionRepository
            .findByIdempotencyKey(
                request.getIdempotencyKey()
            )
            .orElse(null);

    if (existingTransaction != null) {

      return transactionMapper.toResponse(
          existingTransaction
      );
    }

    if (request.getSourceAccountId()
        .equals(request.getDestinationAccountId())) {

      throw new InvalidTransactionException(
          "Source account and destination account cannot be the same."
      );
    }

    if (request.getAmount() == null
        || request.getAmount()
        .compareTo(BigDecimal.ZERO) <= 0) {

      throw new InvalidTransactionException(
          "Transaction amount must be greater than zero."
      );
    }

    AccountResponse sourceAccount =
        getAccount(
            request.getSourceAccountId()
        );

    AccountResponse destinationAccount =
        getAccount(
            request.getDestinationAccountId()
        );

    validateAccountIsActive(
        sourceAccount,
        "Source account"
    );

    validateAccountIsActive(
        destinationAccount,
        "Destination account"
    );

    validateCurrency(
        sourceAccount,
        destinationAccount,
        request
    );

    validateBalance(
        sourceAccount,
        request.getAmount()
    );

    Transaction transaction =
        Transaction.builder()
            .transactionReference(
                generateTransactionReference()
            )
            .sourceAccountId(
                request.getSourceAccountId()
            )
            .destinationAccountId(
                request.getDestinationAccountId()
            )
            .amount(
                request.getAmount()
            )
            .currency(
                request.getCurrency()
            )
            .transactionType(
                request.getTransactionType()
            )
            .status(
                TransactionStatus.PENDING
            )
            .description(
                request.getDescription()
            )
            .channel(
                request.getChannel()
            )
            .ipAddress(
                request.getIpAddress()
            )
            .deviceId(
                request.getDeviceId()
            )
            .location(
                request.getLocation()
            )
            .transactionDate(
                LocalDateTime.now()
            )
            .idempotencyKey(
                request.getIdempotencyKey()
            )
            .build();

    Transaction savedTransaction =
        transactionRepository.save(
            transaction
        );

    createTransactionRequestedOutboxEvent(
        savedTransaction
    );

    return transactionMapper.toResponse(
        savedTransaction
    );
  }

  @Override
  public TransactionResponse getTransaction(
      UUID transactionId) {

    Transaction transaction =
        transactionRepository
            .findById(transactionId)
            .orElseThrow(
                () -> new TransactionNotFoundException(
                    transactionId
                )
            );

    return transactionMapper.toResponse(
        transaction
    );
  }

  @Override
  public TransactionResponse getByReference(
      String transactionReference) {

    Transaction transaction =
        transactionRepository
            .findByTransactionReference(
                transactionReference
            )
            .orElseThrow(
                () -> new TransactionNotFoundException(
                    transactionReference
                )
            );

    return transactionMapper.toResponse(
        transaction
    );
  }

  @Override
  public List<TransactionResponse> getAccountTransactions(
      UUID accountId) {

    List<Transaction> transactions =
        transactionRepository
            .findBySourceAccountId(accountId);

    return transactions.stream()
        .map(transactionMapper::toResponse)
        .toList();
  }

  @Override
  public List<TransactionResponse> getAllTransactions() {

    return transactionRepository
        .findAll()
        .stream()
        .map(transactionMapper::toResponse)
        .toList();
  }

  private AccountResponse getAccount(
      UUID accountId) {

    try {

      return accountClient.getAccount(
          accountId
      );

    } catch (Exception exception) {

      throw new TransactionProcessingException(
          "Failed to retrieve account: "
              + accountId,
          exception
      );
    }
  }

  private void validateAccountIsActive(
      AccountResponse account,
      String accountName) {

    if (account == null) {

      throw new InvalidTransactionException(
          accountName + " does not exist."
      );
    }

    if (!"ACTIVE".equalsIgnoreCase(
        account.getStatus()
    )) {

      throw new InvalidTransactionException(
          accountName + " is not active."
      );
    }
  }

  private void validateCurrency(
      AccountResponse sourceAccount,
      AccountResponse destinationAccount,
      TransactionCreateRequest request) {

    if (sourceAccount.getCurrency()
        != request.getCurrency()) {

      throw new InvalidTransactionException(
          "Transaction currency does not match source account currency."
      );
    }

    if (destinationAccount.getCurrency()
        != request.getCurrency()) {

      throw new InvalidTransactionException(
          "Transaction currency does not match destination account currency."
      );
    }
  }

  private void validateBalance(
      AccountResponse sourceAccount,
      BigDecimal amount) {

    if (sourceAccount.getAvailableBalance() == null) {

      throw new InsufficientBalanceException(
          "Source account has no available balance."
      );
    }

    if (sourceAccount
        .getAvailableBalance()
        .compareTo(amount) < 0) {

      throw new InsufficientBalanceException(
          amount,
          sourceAccount.getAvailableBalance()
      );
    }
  }

  private String generateTransactionReference() {

    return "TXN-"
        + System.currentTimeMillis()
        + "-"
        + UUID.randomUUID()
        .toString()
        .substring(0, 8)
        .toUpperCase();
  }

  private void createTransactionRequestedOutboxEvent(
      Transaction transaction) {

    try {

      TransactionRequestedEvent event =
          TransactionRequestedEvent.builder()
              .transactionId(
                  transaction.getId()
              )
              .transactionReference(
                  transaction.getTransactionReference()
              )
              .sourceAccountId(
                  transaction.getSourceAccountId()
              )
              .destinationAccountId(
                  transaction.getDestinationAccountId()
              )
              .amount(
                  transaction.getAmount()
              )
              .currency(
                  transaction
                      .getCurrency()
                      .name()
              )
              .transactionType(
                  transaction
                      .getTransactionType()
                      .name()
              )
              .channel(
                  transaction.getChannel()
              )
              .ipAddress(
                  transaction.getIpAddress()
              )
              .deviceId(
                  transaction.getDeviceId()
              )
              .location(
                  transaction.getLocation()
              )
              .transactionDate(
                  transaction.getTransactionDate()
              )
              .build();

      String payload =
          objectMapper.writeValueAsString(
              event
          );

      OutboxEvent outboxEvent =
          OutboxEvent.builder()
              .aggregateType(
                  "Transaction"
              )
              .aggregateId(
                  transaction.getId()
              )
              .eventType(
                  "TRANSACTION_REQUESTED"
              )
              .payload(
                  payload
              )
              .published(false)
              .build();

      outboxEventRepository.save(
          outboxEvent
      );

    } catch (JsonProcessingException exception) {

      throw new TransactionProcessingException(
          "Failed to create transaction requested outbox event.",
          exception
      );
    }
  }
  @Override
  @Transactional
  public void processFraudDecision(
      FraudDecisionEvent event) {

    Transaction transaction =
        transactionRepository
            .findById(event.getTransactionId())
            .orElseThrow(
                () ->
                    new TransactionNotFoundException(
                        event.getTransactionId()
                    )
            );

    if (transaction.getStatus() != TransactionStatus.PENDING) {

      log.info(
          "Transaction already processed. " +
              "transactionId={}, status={}",
          transaction.getId(),
          transaction.getStatus()
      );

      return;
    }

    transaction.setFraudScore(
        event.getFraudScore()
    );

    if (event.getDecision() == FraudDecision.BLOCKED) {

      transaction.setStatus(
          TransactionStatus.BLOCKED
      );

      transaction.setFailureReason(
          event.getReason()
      );

      transactionRepository.save(transaction);

      log.warn(
          "Transaction blocked. transactionId={}, score={}, reason={}",
          transaction.getId(),
          event.getFraudScore(),
          event.getReason()
      );

      return;
    }

    if (event.getDecision() == FraudDecision.APPROVED) {

      transaction.setStatus(
          TransactionStatus.PROCESSING
      );

      transactionRepository.save(transaction);

      processApprovedTransaction(transaction);
    }
  }

  private void processApprovedTransaction(
      Transaction transaction) {

    BalanceOperationRequest operationRequest =
        new BalanceOperationRequest(
            transaction.getAmount()
        );

    try {

      accountClient.debit(
          transaction.getSourceAccountId(),
          operationRequest
      );

    } catch (Exception exception) {

      transaction.setStatus(
          TransactionStatus.FAILED
      );

      transactionRepository.save(
          transaction
      );

      log.error(
          "Source account debit failed. " +
              "transactionId={}",
          transaction.getId(),
          exception
      );

      throw new TransactionProcessingException(
          "Source account debit failed.",
          exception
      );
    }

    try {

      accountClient.credit(
          transaction.getDestinationAccountId(),
          operationRequest
      );

    } catch (Exception exception) {

      try {

        accountClient.credit(
            transaction.getSourceAccountId(),
            operationRequest
        );

      } catch (Exception compensationException) {

        transaction.setStatus(
            TransactionStatus.FAILED
        );

        transactionRepository.save(
            transaction
        );

        log.error(
            "Destination credit failed " +
                "and compensation also failed. " +
                "transactionId={}",
            transaction.getId(),
            compensationException
        );

        throw new TransactionProcessingException(
            "Destination credit failed " +
                "and compensation also failed.",
            compensationException
        );
      }

      transaction.setStatus(
          TransactionStatus.FAILED
      );

      transactionRepository.save(
          transaction
      );

      log.error(
          "Destination account credit failed. " +
              "Compensation completed. transactionId={}",
          transaction.getId(),
          exception
      );

      throw new TransactionProcessingException(
          "Destination account credit failed.",
          exception
      );
    }

    transaction.setStatus(
        TransactionStatus.COMPLETED
    );

    Transaction completedTransaction =
        transactionRepository.save(
            transaction
        );

    createTransactionCompletedOutboxEvent(
        completedTransaction
    );


    log.info(
        "Transaction completed successfully. " +
            "transactionId={}, reference={}",
        completedTransaction.getId(),
        completedTransaction.getTransactionReference()
    );
  }

  private void createTransactionCompletedOutboxEvent(
      Transaction transaction) {

    try {

      TransactionCompletedEvent event =
          TransactionCompletedEvent.builder()
              .transactionId(
                  transaction.getId()
              )
              .transactionReference(
                  transaction.getTransactionReference()
              )
              .sourceAccountId(
                  transaction.getSourceAccountId()
              )
              .destinationAccountId(
                  transaction.getDestinationAccountId()
              )
              .amount(
                  transaction.getAmount()
              )
              .currency(
                  transaction.getCurrency().name()
              )
              .transactionType(
                  transaction
                      .getTransactionType()
                      .name()
              )
              .transactionDate(
                  transaction.getTransactionDate()
              )
              .build();

      String payload =
          objectMapper.writeValueAsString(event);

      OutboxEvent outboxEvent =
          OutboxEvent.builder()
              .aggregateType("Transaction")
              .aggregateId(
                  transaction.getId()
              )
              .eventType(
                  "TRANSACTION_COMPLETED"
              )
              .payload(payload)
              .published(false)
              .build();

      outboxEventRepository.save(outboxEvent);

    } catch (JsonProcessingException exception) {

      throw new TransactionProcessingException(
          "Failed to create transaction outbox event.",
          exception
      );
    }
  }
  private void compensateDebit(
      Transaction transaction,
      BalanceOperationRequest request,
      Exception originalException
  ) {

    try {

      accountClient.credit(
          transaction.getSourceAccountId(),
          request
      );

      transaction.setCompensationCompleted(
          true
      );

      transaction.setStatus(
          TransactionStatus.FAILED
      );

      transaction.setFailureReason(
          "Destination credit failed. " +
              "Source account debit was successfully compensated."
      );

      transactionRepository.save(transaction);

      log.info(
          "Compensation completed successfully. " +
              "transactionId={}",
          transaction.getId()
      );

    } catch (Exception compensationException) {

      transaction.setStatus(
          TransactionStatus.COMPENSATION_REQUIRED
      );

      transaction.setFailureReason(
          "Destination credit failed and " +
              "compensation also failed."
      );

      transactionRepository.save(transaction);

      log.error(
          "CRITICAL: Compensation failed. " +
              "transactionId={}",
          transaction.getId(),
          compensationException
      );

      throw new TransactionProcessingException(
          "Transaction requires compensation.",
          compensationException
      );
    }
  }

  @Override
  @Transactional
  public void compensateTransaction(
      UUID transactionId
  ) {

    Transaction transaction =
        transactionRepository
            .findById(transactionId)
            .orElseThrow(
                () ->
                    new TransactionNotFoundException(
                        transactionId
                    )
            );

    if (transaction.getStatus()
        != TransactionStatus.COMPENSATION_REQUIRED) {

      throw new IllegalStateException(
          "Transaction does not require compensation."
      );
    }

    if (!transaction.isDebitCompleted()) {

      throw new IllegalStateException(
          "Debit was not completed. Compensation is not required."
      );
    }

    if (transaction.isCompensationCompleted()) {

      log.info(
          "Compensation already completed. transactionId={}",
          transactionId
      );

      return;
    }

    BalanceOperationRequest request =
        new BalanceOperationRequest(
            transaction.getAmount()
        );

    try {

      accountClient.credit(
          transaction.getSourceAccountId(),
          request
      );

      transaction.setCompensationCompleted(
          true
      );

      transaction.setStatus(
          TransactionStatus.FAILED
      );

      transaction.setFailureReason(
          "Compensation completed successfully."
      );

      transactionRepository.save(transaction);

      log.info(
          "Manual compensation completed. transactionId={}",
          transactionId
      );

    } catch (Exception exception) {

      log.error(
          "Compensation retry failed. transactionId={}",
          transactionId,
          exception
      );

      throw new TransactionProcessingException(
          "Compensation failed again.",
          exception
      );
    }
  }
}