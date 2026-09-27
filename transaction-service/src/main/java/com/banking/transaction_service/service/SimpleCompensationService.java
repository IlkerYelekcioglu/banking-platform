package com.banking.transaction_service.service;

import com.banking.transaction_service.client.AccountClient;
import com.banking.transaction_service.config.CompensationProperties;
import com.banking.transaction_service.dto.request.BalanceOperationRequest;
import com.banking.transaction_service.entity.Transaction;
import com.banking.transaction_service.enums.TransactionStatus;
import com.banking.transaction_service.exception.TransactionNotFoundException;
import com.banking.transaction_service.repository.TransactionRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SimpleCompensationService implements CompensationService{

  private final TransactionRepository transactionRepository;

  private final AccountClient accountClient;

  private final CompensationProperties properties;

  @Override
  public void processPendingCompensations() {

    List<Transaction> transactions =
        transactionRepository
            .findTop100ByStatusAndNextCompensationRetryAtLessThanEqualOrderByNextCompensationRetryAtAsc(
                TransactionStatus.COMPENSATION_REQUIRED,
                LocalDateTime.now()
            );

    if (transactions.isEmpty()) {

      log.debug(
          "No pending compensations found."
      );

      return;
    }

    log.info(
        "Found {} pending compensations.",
        transactions.size()
    );

    for (Transaction transaction : transactions) {

      try {

        retryCompensation(
            transaction.getId()
        );

      } catch (Exception exception) {

        log.error(
            "Compensation retry failed. " +
                "transactionId={}",
            transaction.getId(),
            exception
        );
      }
    }
  }

  @Override
  @Transactional
  public void retryCompensation(UUID transactionId) {

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

      log.info(
          "Transaction no longer requires compensation. " +
              "transactionId={}, status={}",
          transactionId,
          transaction.getStatus()
      );

      return;
    }

    if (transaction.isCompensationCompleted()) {

      log.info(
          "Compensation already completed. " +
              "transactionId={}",
          transactionId
      );

      return;
    }

    if (!transaction.isDebitCompleted()) {

      log.warn(
          "Debit was not completed. " +
              "Skipping compensation. transactionId={}",
          transactionId
      );

      return;
    }

    if (transaction.getCompensationRetryCount()
        >= properties.getMaxRetries()) {

      log.error(
          "Maximum compensation retry count reached. " +
              "transactionId={}, retryCount={}",
          transactionId,
          transaction.getCompensationRetryCount()
      );

      return;
    }

    transaction.setCompensationRetryCount(
        transaction.getCompensationRetryCount() + 1
    );

    transactionRepository.save(transaction);

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

      transaction.setNextCompensationRetryAt(
          null
      );

      transaction.setCompensationFailureReason(
          null
      );

      transaction.setFailureReason(
          "Destination credit failed. " +
              "Source account was successfully compensated."
      );

      transactionRepository.save(transaction);

      log.info(
          "Compensation completed successfully. " +
              "transactionId={}, retryCount={}",
          transactionId,
          transaction.getCompensationRetryCount()
      );

    } catch (Exception exception) {

      handleCompensationFailure(
          transaction,
          exception
      );
    }
  }

  private void handleCompensationFailure(
      Transaction transaction,
      Exception exception
  ) {

    int retryCount =
        transaction.getCompensationRetryCount();

    transaction.setCompensationFailureReason(
        exception.getMessage()
    );

    if (retryCount >= properties.getMaxRetries()) {

      transaction.setNextCompensationRetryAt(
          null
      );

      transactionRepository.save(transaction);

      log.error(
          "Maximum compensation retries reached. " +
              "Manual intervention required. " +
              "transactionId={}",
          transaction.getId(),
          exception
      );

      return;
    }

    LocalDateTime nextRetry =
        calculateNextRetryTime(
            retryCount
        );

    transaction.setNextCompensationRetryAt(
        nextRetry
    );

    transactionRepository.save(transaction);

    log.warn(
        "Compensation failed. " +
            "transactionId={}, retryCount={}, nextRetryAt={}",
        transaction.getId(),
        retryCount,
        nextRetry,
        exception
    );
  }

  private LocalDateTime calculateNextRetryTime(
      int retryCount
  ) {

    long delay =
        (long)
            properties.getInitialDelaySeconds()
            * (1L << Math.max(
            0,
            retryCount - 1
        ));

    delay =
        Math.min(
            delay,
            properties.getMaxDelaySeconds()
        );

    return LocalDateTime.now()
        .plusSeconds(delay);
  }
}