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

  private final SimpleCompensationClaimService simpleCompensationClaimService;

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

        boolean claimed =
            simpleCompensationClaimService.claim(
                transaction.getId()
            );

        if (!claimed) {

          log.debug(
              "Compensation already claimed by another instance. " +
                  "transactionId={}",
              transaction.getId()
          );

          continue;
        }
        retryCompensation(
            transaction.getId()
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
  public void retryCompensation(
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
        != TransactionStatus.COMPENSATING) {

      log.info(
          "Transaction is not in COMPENSATING state. " +
              "transactionId={}, status={}",
          transactionId,
          transaction.getStatus()
      );

      return;
    }

    if (!transaction.isDebitCompleted()) {

      log.error(
          "Invalid compensation state. " +
              "Debit was not completed. transactionId={}",
          transactionId
      );

      transaction.setStatus(
          TransactionStatus.FAILED
      );

      transaction.setFailureReason(
          "Invalid compensation state: debit was not completed."
      );

      transactionRepository.save(transaction);

      return;
    }

    if (transaction.isCompensationCompleted()) {

      log.info(
          "Compensation already completed. " +
              "transactionId={}",
          transactionId
      );

      transaction.setStatus(
          TransactionStatus.FAILED
      );

      transactionRepository.save(transaction);

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

      log.info(
          "Starting compensation. " +
              "transactionId={}, retryCount={}",
          transactionId,
          transaction.getCompensationRetryCount()
      );

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

      transaction.setCompensationClaimedAt(
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

      transaction.setStatus(
          TransactionStatus.COMPENSATION_REQUIRED
      );

      transaction.setNextCompensationRetryAt(
          null
      );

      transaction.setCompensationClaimedAt(
          null
      );

      transaction.setFailureReason(
          "Maximum compensation retries reached. " +
              "Manual intervention required."
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

    transaction.setStatus(
        TransactionStatus.COMPENSATION_REQUIRED
    );

    transaction.setNextCompensationRetryAt(
        nextRetry
    );

    transaction.setCompensationClaimedAt(
        null
    );

    transaction.setFailureReason(
        "Compensation failed. " +
            "Retry scheduled."
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


  @Override
  public void recoverStuckCompensations() {

    LocalDateTime threshold =
        LocalDateTime.now()
            .minusSeconds(
                properties.getClaimTimeoutSeconds()
            );

    List<Transaction> stuckTransactions =
        transactionRepository
            .findStuckCompensations(
                TransactionStatus.COMPENSATING,
                threshold
            );

    for (Transaction transaction : stuckTransactions) {

      try {

        int updatedRows =
            transactionRepository
                .releaseStuckCompensation(
                    transaction.getId(),
                    TransactionStatus.COMPENSATING,
                    TransactionStatus.COMPENSATION_REQUIRED,
                    LocalDateTime.now(),
                    LocalDateTime.now()
                );

        if (updatedRows == 1) {

          log.warn(
              "Stuck compensation released. " +
                  "transactionId={}",
              transaction.getId()
          );
        }

      } catch (Exception exception) {

        log.error(
            "Failed to release stuck compensation. " +
                "transactionId={}",
            transaction.getId(),
            exception
        );
      }
    }
  }
}