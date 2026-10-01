package com.banking.transaction_service.repository;

import com.banking.transaction_service.entity.OutboxEvent;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

  List<OutboxEvent>findTop100ByPublishedFalseAndClaimedAtIsNullOrderByCreatedAtAsc();

  @Modifying(
      clearAutomatically = true,
      flushAutomatically = true
  )
  @Query("""
        UPDATE OutboxEvent o
        SET o.claimedAt = :claimedAt
        WHERE
            o.id = :eventId
            AND o.published = false
            AND o.claimedAt IS NULL
    """)
  int claimEvent(@Param("eventId") UUID eventId, @Param("claimedAt") LocalDateTime claimedAt);

  @Modifying(
      clearAutomatically = true,
      flushAutomatically = true
  )
  @Query("""
        UPDATE OutboxEvent o
        SET
            o.published = true,
            o.publishedAt = :publishedAt,
            o.claimedAt = null
        WHERE
            o.id = :eventId
            AND o.published = false
    """)
  int markAsPublished(@Param("eventId") UUID eventId, @Param("publishedAt") LocalDateTime publishedAt);

  @Modifying(
      clearAutomatically = true,
      flushAutomatically = true
  )
  @Query("""
        UPDATE OutboxEvent o
        SET
            o.claimedAt = null
        WHERE
            o.id = :eventId
            AND o.published = false
            AND o.claimedAt IS NOT NULL
    """)
  int releaseClaim(@Param("eventId") UUID eventId);

  List<OutboxEvent> findTop100ByPublishedFalseAndClaimedAtLessThanEqualOrderByClaimedAtAsc(LocalDateTime threshold);


  @Modifying(
      clearAutomatically = true,
      flushAutomatically = true
  )
  @Query("""
    UPDATE OutboxEvent o
    SET
        o.permanentlyFailed = false,
        o.retryCount = 0,
        o.nextRetryAt = null,
        o.failureReason = null,
        o.claimedAt = null
    WHERE
        o.id = :eventId
        AND o.published = false
        AND o.permanentlyFailed = true
""")
  int resetPermanentlyFailed(
      @Param("eventId") UUID eventId
  );

  @Modifying(
      clearAutomatically = true,
      flushAutomatically = true
  )
  @Query("""
        UPDATE OutboxEvent o
        SET
            o.retryCount = :retryCount,
            o.nextRetryAt = :nextRetryAt,
            o.failureReason = :failureReason,
            o.claimedAt = null
        WHERE
            o.id = :eventId
            AND o.published = false
            AND o.permanentlyFailed = false
    """)
  int scheduleRetry(
      @Param("eventId") UUID eventId,
      @Param("retryCount") int retryCount,
      @Param("nextRetryAt") LocalDateTime nextRetryAt,
      @Param("failureReason") String failureReason
  );

  @Modifying(
      clearAutomatically = true,
      flushAutomatically = true
  )
  @Query("""
        UPDATE OutboxEvent o
        SET
            o.retryCount = :retryCount,
            o.failureReason = :failureReason,
            o.claimedAt = null,
            o.permanentlyFailed = true
        WHERE
            o.id = :eventId
            AND o.published = false
    """)
  int markAsPermanentlyFailed(
      @Param("eventId") UUID eventId,
      @Param("retryCount") int retryCount,
      @Param("failureReason") String failureReason
  );

  List<OutboxEvent> findTop100ByPublishedFalseAndPermanentlyFailedFalseAndClaimedAtLessThanEqualOrderByClaimedAtAsc(LocalDateTime threshold);

  List<OutboxEvent> findFirst100ByPublishedFalseAndPermanentlyFailedFalseAndClaimedAtIsNullAndNextRetryAtIsNullOrderByCreatedAtAsc();

  List<OutboxEvent> findFirst100ByPublishedFalseAndPermanentlyFailedFalseAndClaimedAtIsNullAndNextRetryAtLessThanEqualOrderByNextRetryAtAsc(
      LocalDateTime maxNextRetryAt
  );
}
