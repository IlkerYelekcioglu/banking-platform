package com.banking.transaction_service.service;


import com.banking.transaction_service.client.AccountClient;
import com.banking.transaction_service.config.CompensationProperties;
import com.banking.transaction_service.dto.request.BalanceOperationRequest;
import com.banking.transaction_service.entity.Transaction;
import com.banking.transaction_service.enums.TransactionStatus;
import com.banking.transaction_service.exception.TransactionNotFoundException;
import com.banking.transaction_service.repository.TransactionRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SimpleCompensationService
    implements CompensationService {

  private final TransactionRepository transactionRepository;

  private final AccountClient accountClient;

  private final SimpleCompensationClaimService compensationClaimService;

  private final CompensationProperties compensationProperties;

  private final CompensationRetryPolicy retryPolicy;

  @Override
  @Transactional(readOnly = true)
  public void processPendingCompensations() {

    LocalDateTime now =
        LocalDateTime.now();

    List<Transaction> transactions =
        transactionRepository
            .findTop100ByStatusAndNextCompensationRetryAtLessThanEqualOrderByNextCompensationRetryAtAsc(
                TransactionStatus.COMPENSATION_REQUIRED,
                now
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

      String claimToken =
          compensationClaimService.claim(
              transaction.getId()
          );

      if (claimToken == null) {

        log.debug(
            "Compensation was already claimed " +
                "or transaction is no longer eligible. " +
                "transactionId={}",
            transaction.getId()
        );

        continue;
      }

      try {

        retryCompensation(
            transaction.getId(),
            claimToken
        );

      } catch (Exception exception) {

        log.error(
            "Compensation processing failed. " +
                "transactionId={}",
            transaction.getId(),
            exception
        );
      }
    }
  }

  @Override
  @Transactional
  public void recoverStuckCompensations() {

    LocalDateTime timeout =
        LocalDateTime.now()
            .minusSeconds(
                compensationProperties
                    .getClaimTimeoutSeconds()
            );

    List<Transaction> stuckTransactions =
        transactionRepository
            .findByStatusAndCompensationClaimedAtBefore(
                TransactionStatus.COMPENSATING,
                timeout
            );

    if (stuckTransactions.isEmpty()) {

      log.debug(
          "No stuck compensations found."
      );

      return;
    }

    log.warn(
        "Found {} stuck compensations.",
        stuckTransactions.size()
    );

    for (Transaction transaction
        : stuckTransactions) {

      log.warn(
          "Recovering stuck compensation. " +
              "transactionId={}",
          transaction.getId()
      );

      transaction.setStatus(
          TransactionStatus.COMPENSATION_REQUIRED
      );

      transaction.setCompensationClaimedAt(
          null
      );

      transaction.setCompensationClaimToken(
          null
      );

      transaction.setNextCompensationRetryAt(
          LocalDateTime.now()
      );

      transaction.setCompensationFailureReason(
          "Compensation claim timed out " +
              "and was recovered."
      );

      transactionRepository.save(
          transaction
      );
    }
  }

  @Override
  @Transactional
  public void retryCompensation(
      UUID transactionId,
      String claimToken) {

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
        != TransactionStatus.COMPENSATING) {

      log.warn(
          "Transaction is not in COMPENSATING state. " +
              "transactionId={}, status={}",
          transactionId,
          transaction.getStatus()
      );

      return;
    }

    if (!claimToken.equals(
        transaction.getCompensationClaimToken()
    )) {

      log.warn(
          "Invalid compensation claim token. " +
              "transactionId={}",
          transactionId
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
              "Compensation is not required. " +
              "transactionId={}",
          transactionId
      );

      transaction.setStatus(
          TransactionStatus.FAILED
      );

      transaction.setCompensationClaimedAt(
          null
      );

      transaction.setCompensationClaimToken(
          null
      );

      transactionRepository.save(
          transaction
      );

      return;
    }

    BalanceOperationRequest request =
        new BalanceOperationRequest(
            transaction.getAmount()
        );

    String operationKey =
        "TX-"
            + transaction.getId()
            + "-COMPENSATION";

    try {

      log.info(
          "Starting compensation. " +
              "transactionId={}, retryCount={}, operationKey={}",
          transaction.getId(),
          transaction.getCompensationRetryCount(),
          operationKey
      );

      accountClient.credit(
          transaction.getSourceAccountId(),
          request,
          operationKey
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

      transaction.setCompensationClaimedAt(
          null
      );

      transaction.setCompensationClaimToken(
          null
      );

      transaction.setCompensationFailureReason(
          null
      );

      transaction.setFailureReason(
          "Destination credit failed. " +
              "Source account debit was successfully compensated."
      );

      transactionRepository.save(
          transaction
      );

      log.info(
          "Compensation completed successfully. " +
              "transactionId={}",
          transaction.getId()
      );

    } catch (Exception exception) {

      log.error(
          "Compensation failed. " +
              "transactionId={}, retryCount={}",
          transaction.getId(),
          transaction.getCompensationRetryCount(),
          exception
      );

      handleCompensationFailure(
          transaction,
          claimToken,
          exception
      );
    }
  }

  private void handleCompensationFailure(
      Transaction transaction,
      String claimToken,
      Exception exception) {

    int nextRetryCount =
        transaction.getCompensationRetryCount()
            + 1;

    transaction.setCompensationRetryCount(
        nextRetryCount
    );

    transaction.setCompensationFailureReason(
        buildFailureReason(exception)
    );

    if (retryPolicy.hasRetriesRemaining(
        nextRetryCount
    )) {

      Duration delay =
          retryPolicy.calculateDelay(
              nextRetryCount
          );

      LocalDateTime retryAt =
          LocalDateTime.now()
              .plus(delay);

      transaction.setStatus(
          TransactionStatus.COMPENSATION_REQUIRED
      );

      transaction.setNextCompensationRetryAt(
          retryAt
      );

      transaction.setCompensationClaimedAt(
          null
      );

      transaction.setCompensationClaimToken(
          null
      );

      transactionRepository.save(
          transaction
      );

      log.warn(
          "Compensation scheduled for retry. " +
              "transactionId={}, retryCount={}, retryAt={}",
          transaction.getId(),
          nextRetryCount,
          retryAt
      );

      return;
    }

    transaction.setStatus(
        TransactionStatus.COMPENSATION_REQUIRED
    );

    transaction.setNextCompensationRetryAt(
        null
    );

    transaction.setCompensationClaimedAt(
        null
    );

    transaction.setCompensationClaimToken(
        null
    );

    transaction.setCompensationFailureReason(
        "Maximum compensation retry count exceeded. " +
            "Manual intervention is required."
    );

    transactionRepository.save(
        transaction
    );

    log.error(
        "CRITICAL: Maximum compensation retries exceeded. " +
            "Manual intervention required. " +
            "transactionId={}, retryCount={}",
        transaction.getId(),
        nextRetryCount
    );
  }


  // ============================================================
  // 5. MANUAL COMPENSATION
  // ============================================================

  @Override
  @Transactional
  public void manuallyCompensate(
      UUID transactionId) {

    Transaction transaction =
        transactionRepository
            .findById(transactionId)
            .orElseThrow(
                () ->
                    new TransactionNotFoundException(
                        transactionId
                    )
            );

    /*
     * Sadece compensation gereken transaction
     * manuel olarak telafi edilebilir.
     */
    if (transaction.getStatus()
        != TransactionStatus.COMPENSATION_REQUIRED) {

      throw new IllegalStateException(
          "Transaction does not require compensation."
      );
    }

    if (!transaction.isDebitCompleted()) {

      throw new IllegalStateException(
          "Debit was not completed. " +
              "Compensation is not required."
      );
    }

    if (transaction.isCompensationCompleted()) {

      log.info(
          "Compensation already completed. " +
              "transactionId={}",
          transactionId
      );

      return;
    }

    BalanceOperationRequest request =
        new BalanceOperationRequest(
            transaction.getAmount()
        );

    /*
     * Manual compensation'da da AYNI
     * idempotency key kullanılmalı.
     */
    String operationKey =
        "TX-"
            + transaction.getId()
            + "-COMPENSATION";

    try {

      log.warn(
          "Starting manual compensation. " +
              "transactionId={}, operationKey={}",
          transactionId,
          operationKey
      );

      accountClient.credit(
          transaction.getSourceAccountId(),
          request,
          operationKey
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

      transaction.setCompensationClaimedAt(
          null
      );

      transaction.setCompensationClaimToken(
          null
      );

      transaction.setCompensationFailureReason(
          null
      );

      transaction.setFailureReason(
          "Manual compensation completed successfully."
      );

      transactionRepository.save(
          transaction
      );

      log.info(
          "Manual compensation completed. " +
              "transactionId={}",
          transactionId
      );

    } catch (Exception exception) {

      transaction.setCompensationFailureReason(
          "Manual compensation failed: "
              + buildFailureReason(exception)
      );

      transactionRepository.save(
          transaction
      );

      log.error(
          "Manual compensation failed. " +
              "transactionId={}",
          transactionId,
          exception
      );

      throw exception;
    }
  }


  // ============================================================
  // 6. FAILURE MESSAGE
  // ============================================================

  private String buildFailureReason(
      Exception exception) {

    String message =
        exception.getMessage();

    if (message == null
        || message.isBlank()) {

      return exception
          .getClass()
          .getSimpleName();
    }

    /*
     * Database'de gereğinden fazla uzun
     * exception message tutmamak için.
     */
    if (message.length() > 1000) {

      return message.substring(
          0,
          1000
      );
    }

    return message;
  }
}