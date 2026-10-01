package com.banking.transaction_service.service;

import com.banking.transaction_service.client.AccountClient;
import com.banking.transaction_service.config.CompensationProperties;
import com.banking.transaction_service.dto.request.BalanceOperationRequest;
import com.banking.transaction_service.entity.Transaction;
import com.banking.transaction_service.enums.TransactionStatus;
import com.banking.transaction_service.exception.TransactionNotFoundException;
import com.banking.transaction_service.repository.TransactionRepository;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Service
@RequiredArgsConstructor
@Slf4j
public class SimpleCompensationService
    implements CompensationService {

  private final TransactionRepository transactionRepository;
  private final AccountClient accountClient;
  private final SimpleCompensationClaimService
      compensationClaimService;
  private final CompensationProperties
      compensationProperties;


  @Override
  @Transactional(readOnly = true)
  public void processPendingCompensations() {

    List<Transaction> transactions =
        transactionRepository
            .findTop100ByStatusAndNextCompensationRetryAtLessThanEqualOrderByNextCompensationRetryAtAsc(
                TransactionStatus.COMPENSATION_REQUIRED,
                LocalDateTime.now()
            );

    for (Transaction transaction : transactions) {

      String claimToken =
          compensationClaimService.claim(
              transaction.getId()
          );

      if (claimToken == null) {

        log.debug(
            "Compensation claim failed. " +
                "transactionId={}",
            transaction.getId()
        );

        continue;
      }

      retryCompensation(
          transaction.getId(),
          claimToken
      );
    }
  }


  @Override
  @Transactional
  public void retryCompensation(
      UUID transactionId,
      String claimToken
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

    if (!transaction.isDebitCompleted()) {

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

    if (transaction.isCompensationCompleted()) {
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

      transactionRepository.save(
          transaction
      );

      log.info(
          "Compensation completed. " +
              "transactionId={}, operationKey={}",
          transactionId,
          operationKey
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

    int nextRetryCount =
        transaction.getCompensationRetryCount()
            + 1;

    transaction.setCompensationRetryCount(
        nextRetryCount
    );

    transaction.setCompensationFailureReason(
        buildFailureReason(exception)
    );

    transaction.setCompensationClaimedAt(
        null
    );

    transaction.setCompensationClaimToken(
        null
    );

    if (nextRetryCount
        >= compensationProperties.getMaxRetries()) {

      transaction.setStatus(
          TransactionStatus.COMPENSATION_REQUIRED
      );

      transaction.setNextCompensationRetryAt(
          null
      );

      transaction.setFailureReason(
          "Compensation failed after maximum retries. "
              + "Manual intervention required."
      );

      transactionRepository.save(
          transaction
      );

      log.error(
          "Compensation permanently failed. " +
              "transactionId={}, retryCount={}",
          transaction.getId(),
          nextRetryCount,
          exception
      );

      return;
    }

    long delaySeconds =
        calculateDelaySeconds(
            nextRetryCount
        );

    transaction.setStatus(
        TransactionStatus.COMPENSATION_REQUIRED
    );

    transaction.setNextCompensationRetryAt(
        LocalDateTime.now()
            .plusSeconds(delaySeconds)
    );

    transactionRepository.save(
        transaction
    );

    log.warn(
        "Compensation retry scheduled. " +
            "transactionId={}, retryCount={}, nextRetryAt={}",
        transaction.getId(),
        nextRetryCount,
        transaction.getNextCompensationRetryAt()
    );
  }


  private long calculateDelaySeconds(
      int retryCount
  ) {

    long delay =
        (long)
            compensationProperties
                .getInitialDelaySeconds()
            *
            (1L << Math.max(
                0,
                retryCount - 1
            ));

    return Math.min(
        delay,
        compensationProperties
            .getMaxDelaySeconds()
    );
  }


  private String buildFailureReason(
      Exception exception
  ) {

    String message =
        exception.getMessage();

    if (message == null
        || message.isBlank()) {

      return exception
          .getClass()
          .getSimpleName();
    }

    return message.length() > 1000
        ? message.substring(0, 1000)
        : message;
  }

  @Override
  @Transactional
  public void recoverStuckCompensations() {

    LocalDateTime threshold =
        LocalDateTime.now()
            .minusSeconds(
                compensationProperties
                    .getClaimTimeoutSeconds()
            );

    List<Transaction> transactions =
        transactionRepository
            .findStuckCompensations(
                TransactionStatus.COMPENSATING,
                threshold
            );

    for (Transaction transaction :
        transactions) {

      String claimToken =
          transaction.getCompensationClaimToken();

      int updatedRows =
          transactionRepository
              .releaseCompensation(
                  transaction.getId(),
                  TransactionStatus.COMPENSATING,
                  TransactionStatus.COMPENSATION_REQUIRED,
                  LocalDateTime.now(),
                  "Compensation claim timed out. Retrying.",
                  claimToken
              );

      if (updatedRows == 1) {

        log.warn(
            "Stuck compensation released. " +
                "transactionId={}, claimToken={}",
            transaction.getId(),
            claimToken
        );
      }
    }
  }


  @Override
  public void manuallyCompensate(UUID transactionId) {
    Transaction transaction =
        transactionRepository
            .findById(transactionId)
            .orElseThrow(
                () ->
                    new TransactionNotFoundException(
                        transactionId
                    )
            );

    if (!transaction.isDebitCompleted()) {

      throw new IllegalStateException(
          "Transaction does not require compensation."
      );
    }

    if (transaction.isCompensationCompleted()) {

      return;
    }

    if (transaction.getStatus()
        != TransactionStatus.COMPENSATION_REQUIRED) {

      throw new IllegalStateException(
          "Transaction is not ready for compensation. "
              + "Current status="
              + transaction.getStatus()
      );
    }

    String claimToken =
        compensationClaimService.claim(
            transactionId
        );

    if (claimToken == null) {

      throw new IllegalStateException(
          "Transaction could not be claimed for compensation."
      );
    }

    retryCompensation(
        transactionId,
        claimToken
    );

  }
}