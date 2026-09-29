package com.banking.fraud_service.repository;

import com.banking.fraud_service.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.UUID;

public interface ProcessedEventRepository
    extends JpaRepository<ProcessedEvent, UUID> {

  @Modifying
  @Query(
      value = """
                    INSERT INTO processed_events (
                        id,
                        event_id,
                        event_type,
                        aggregate_id,
                        consumer_name,
                        processed_at
                    )
                    VALUES (
                        gen_random_uuid(),
                        :eventId,
                        :eventType,
                        :aggregateId,
                        :consumerName,
                        :processedAt
                    )
                    ON CONFLICT (
                        event_id,
                        consumer_name
                    )
                    DO NOTHING
                    """,
      nativeQuery = true
  )
  int tryMarkAsProcessed(
      @Param("eventId") UUID eventId,
      @Param("eventType") String eventType,
      @Param("aggregateId") UUID aggregateId,
      @Param("consumerName") String consumerName,
      @Param("processedAt") LocalDateTime processedAt
  );
}