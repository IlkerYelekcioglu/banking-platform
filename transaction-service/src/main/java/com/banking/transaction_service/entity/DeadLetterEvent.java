package com.banking.transaction_service.entity;

import com.banking.transaction_service.base.BaseEntity;
import com.banking.transaction_service.enums.DeadLetterStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "dead_letter_events",
    indexes = {
        @Index(
            name = "idx_dlt_status",
            columnList = "status"
        ),
        @Index(
            name = "idx_dlt_original_topic",
            columnList = "original_topic"
        ),
        @Index(
            name = "idx_dlt_created_at",
            columnList = "created_at"
        )
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeadLetterEvent extends BaseEntity {

  @Column(
      name = "original_topic",
      nullable = false,
      length = 200
  )
  private String originalTopic;

  @Column(
      name = "original_partition",
      nullable = false
  )
  private Integer originalPartition;

  @Column(
      name = "original_offset",
      nullable = false
  )
  private Long originalOffset;

  @Column(
      name = "message_key",
      length = 255
  )
  private String messageKey;

  @Column(
      name = "payload",
      nullable = false,
      columnDefinition = "TEXT"
  )
  private String payload;

  @Column(
      name = "exception_type",
      length = 500
  )
  private String exceptionType;

  @Column(
      name = "exception_message",
      length = 2000
  )
  private String exceptionMessage;

  @Column(
      name = "consumer_group",
      length = 200
  )
  private String consumerGroup;

  @Enumerated(EnumType.STRING)
  @Column(
      nullable = false,
      length = 30
  )
  @Builder.Default
  private DeadLetterStatus status =
      DeadLetterStatus.FAILED;

  private LocalDateTime replayedAt;

  @Column(
      name = "replay_count",
      nullable = false
  )
  @Builder.Default
  private int replayCount = 0;

  @Version
  private Long version;
}