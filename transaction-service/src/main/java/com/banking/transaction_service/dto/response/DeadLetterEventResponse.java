package com.banking.transaction_service.dto.response;

import com.banking.transaction_service.enums.DeadLetterStatus;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DeadLetterEventResponse {

  private UUID id;

  private String originalTopic;

  private Integer originalPartition;

  private Long originalOffset;

  private String messageKey;

  private String payload;

  private String exceptionType;

  private String exceptionMessage;

  private String consumerGroup;

  private DeadLetterStatus status;

  private LocalDateTime replayedAt;

  private int replayCount;

  private LocalDateTime createdAt;

  private LocalDateTime updatedAt;
}