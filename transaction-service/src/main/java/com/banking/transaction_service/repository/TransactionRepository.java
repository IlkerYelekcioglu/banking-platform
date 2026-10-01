package com.banking.transaction_service.repository;

import com.banking.transaction_service.entity.Transaction;
import com.banking.transaction_service.enums.TransactionStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

  Optional<Transaction> findByTransactionReference(String transactionReference);

  Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

  List<Transaction> findBySourceAccountId(UUID sourceAccountId);

  List<Transaction> findByDestinationAccountId(UUID destinationAccountId);

  List<Transaction> findBySourceAccountIdAndStatus(UUID sourceAccountId, TransactionStatus status);

  boolean existsByIdempotencyKey(String idempotencyKey);

  List<Transaction> findByStatus(TransactionStatus status);

  List<Transaction> findTop100ByStatusAndNextCompensationRetryAtLessThanEqualOrderByNextCompensationRetryAtAsc(TransactionStatus status, LocalDateTime currentTime);

  @Modifying(
      clearAutomatically = true,
      flushAutomatically = true
  )
  @Query("""
    UPDATE Transaction t
    SET
        t.status = :compensatingStatus,
        t.compensationClaimedAt = :claimedAt,
        t.compensationClaimToken = :claimToken
    WHERE
        t.id = :transactionId
        AND t.status = :requiredStatus
        AND t.compensationCompleted = false
""")
  int claimCompensation(@Param("transactionId") UUID transactionId, @Param("requiredStatus") TransactionStatus requiredStatus, @Param("compensatingStatus") TransactionStatus compensatingStatus, @Param("claimedAt") LocalDateTime claimedAt, @Param("claimToken") String claimToken);
  @Query("""
    SELECT t
    FROM Transaction t
    WHERE
        t.status = :status
        AND t.compensationClaimedAt <= :threshold
""")
  List<Transaction> findStuckCompensations(@Param("status") TransactionStatus status, @Param("threshold") LocalDateTime threshold);


  @Modifying(
      clearAutomatically = true,
      flushAutomatically = true
  )
  @Query("""
    UPDATE Transaction t
    SET
        t.status = :requiredStatus,
        t.nextCompensationRetryAt = :retryAt,
        t.compensationClaimedAt = null,
        t.compensationFailureReason =
            'Compensation claim timed out. Retrying.'
    WHERE
        t.id = :transactionId
        AND t.status = :compensatingStatus
        AND t.compensationClaimedAt <= :threshold
""")
  int releaseStuckCompensation(@Param("transactionId") UUID transactionId,@Param("compensatingStatus") TransactionStatus compensatingStatus, @Param("requiredStatus") TransactionStatus requiredStatus, @Param("threshold") LocalDateTime threshold, @Param("retryAt") LocalDateTime retryAt);

  @Modifying(
      clearAutomatically = true,
      flushAutomatically = true
  )
  @Query("""
    UPDATE Transaction t
    SET
        t.status = :requiredStatus,
        t.nextCompensationRetryAt = :retryAt,
        t.compensationClaimedAt = null,
        t.compensationClaimToken = null,
        t.compensationFailureReason =
            :failureReason
    WHERE
        t.id = :transactionId
        AND t.status = :compensatingStatus
        AND t.compensationClaimToken = :claimToken
""")
  int releaseCompensation(@Param("transactionId") UUID transactionId, @Param("compensatingStatus") TransactionStatus compensatingStatus, @Param("requiredStatus") TransactionStatus requiredStatus, @Param("retryAt") LocalDateTime retryAt, @Param("failureReason") String failureReason, @Param("claimToken") String claimToken);
}

