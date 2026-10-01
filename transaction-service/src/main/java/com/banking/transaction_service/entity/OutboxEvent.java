package com.banking.transaction_service.entity;

import com.banking.transaction_service.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "outbox_events",
    indexes = {
        @Index(
            name = "idx_outbox_published",
            columnList = "published"
        ),
        @Index(
            name = "idx_outbox_created_at",
            columnList = "createdAt"
        ),
        @Index(
            name = "idx_outbox_claimed_at",
            columnList = "claimed_at"
        )
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent extends BaseEntity {

  @Column(nullable = false, length = 100)
  private String aggregateType;

  @Column(nullable = false)
  private UUID aggregateId;

  @Column(nullable = false, length = 100)
  private String eventType;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String payload;

  @Column(nullable = false)
  @Builder.Default
  private boolean published = false;

  private LocalDateTime publishedAt;

  @Column(name = "claimed_at")
  private LocalDateTime claimedAt;

  @Column(nullable = false)
  @Builder.Default
  private int retryCount = 0;

  @Column(name = "next_retry_at")
  private LocalDateTime nextRetryAt;

  @Column(length = 1000)
  private String failureReason;

  @Column(nullable = false)
  @Builder.Default
  private boolean permanentlyFailed = false;

}