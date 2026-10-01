package com.banking.transaction_service.service;

import com.banking.transaction_service.dto.response.DeadLetterEventResponse;
import com.banking.transaction_service.entity.DeadLetterEvent;
import com.banking.transaction_service.enums.DeadLetterStatus;
import com.banking.transaction_service.repository.DeadLetterEventRepository;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class SimpleDeadLetterEventService
    implements DeadLetterEventService {

  private final DeadLetterEventRepository
      deadLetterEventRepository;

  private final KafkaTemplate<String, String>
      kafkaTemplate;


  @Override
  @Transactional
  public void saveDeadLetterEvent(
      String dltTopic,
      int partition,
      long offset,
      String key,
      String payload,
      String exceptionType,
      String exceptionMessage,
      String consumerGroup
  ) {

    String originalTopic =
        resolveOriginalTopic(dltTopic);

    boolean alreadyExists =
        deadLetterEventRepository
            .existsByOriginalTopicAndOriginalPartitionAndOriginalOffset(
                originalTopic,
                partition,
                offset
            );

    if (alreadyExists) {
      return;
    }

    DeadLetterEvent event =
        DeadLetterEvent.builder()
            .originalTopic(originalTopic)
            .originalPartition(partition)
            .originalOffset(offset)
            .messageKey(key)
            .payload(payload)
            .exceptionType(exceptionType)
            .exceptionMessage(
                truncate(
                    exceptionMessage,
                    2000
                )
            )
            .consumerGroup(consumerGroup)
            .status(DeadLetterStatus.FAILED)
            .replayCount(0)
            .build();

    deadLetterEventRepository.save(event);
  }


  @Override
  @Transactional(readOnly = true)
  public Page<DeadLetterEventResponse> getDeadLetterEvents(
      Pageable pageable
  ) {

    return deadLetterEventRepository
        .findAll(pageable)
        .map(this::toResponse);
  }


  @Override
  @Transactional(readOnly = true)
  public Page<DeadLetterEventResponse>
  getFailedDeadLetterEvents(
      Pageable pageable
  ) {

    return deadLetterEventRepository
        .findByStatus(
            DeadLetterStatus.FAILED,
            pageable
        )
        .map(this::toResponse);
  }


  @Override
  @Transactional
  public DeadLetterEventResponse replay(
      UUID deadLetterEventId
  ) {

    DeadLetterEvent event =
        deadLetterEventRepository
            .findById(deadLetterEventId)
            .orElseThrow(
                () -> new IllegalArgumentException(
                    "Dead letter event not found: "
                        + deadLetterEventId
                )
            );

    if (event.getStatus()
        == DeadLetterStatus.REPLAYED) {

      throw new IllegalStateException(
          "Dead letter event has already been replayed."
      );
    }

    try {

      RecordMetadata metadata =
          kafkaTemplate
              .send(
                  event.getOriginalTopic(),
                  event.getMessageKey(),
                  event.getPayload()
              )
              .get(
                  10,
                  TimeUnit.SECONDS
              )
              .getRecordMetadata();

      event.setStatus(
          DeadLetterStatus.REPLAYED
      );

      event.setReplayedAt(
          LocalDateTime.now()
      );

      event.setReplayCount(
          event.getReplayCount() + 1
      );

      DeadLetterEvent saved =
          deadLetterEventRepository.save(
              event
          );

      return toResponse(saved);

    } catch (Exception exception) {

      throw new IllegalStateException(
          "Dead letter replay failed. "
              + "eventId="
              + deadLetterEventId,
          exception
      );
    }
  }


  private String resolveOriginalTopic(
      String dltTopic
  ) {

    if (dltTopic.endsWith(".DLT")) {

      return dltTopic.substring(
          0,
          dltTopic.length() - 4
      );
    }

    return dltTopic;
  }


  private String truncate(
      String value,
      int maxLength
  ) {

    if (value == null) {
      return null;
    }

    if (value.length() <= maxLength) {
      return value;
    }

    return value.substring(
        0,
        maxLength
    );
  }


  private DeadLetterEventResponse toResponse(
      DeadLetterEvent event
  ) {

    return DeadLetterEventResponse.builder()
        .id(event.getId())
        .originalTopic(
            event.getOriginalTopic()
        )
        .originalPartition(
            event.getOriginalPartition()
        )
        .originalOffset(
            event.getOriginalOffset()
        )
        .messageKey(
            event.getMessageKey()
        )
        .payload(
            event.getPayload()
        )
        .exceptionType(
            event.getExceptionType()
        )
        .exceptionMessage(
            event.getExceptionMessage()
        )
        .consumerGroup(
            event.getConsumerGroup()
        )
        .status(
            event.getStatus()
        )
        .replayedAt(
            event.getReplayedAt()
        )
        .replayCount(
            event.getReplayCount()
        )
        .createdAt(
            event.getCreatedAt()
        )
        .updatedAt(
            event.getUpdatedAt()
        )
        .build();
  }
}