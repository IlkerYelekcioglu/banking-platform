package com.banking.transaction_service.kafka;

import com.banking.transaction_service.entity.OutboxEvent;
import com.banking.transaction_service.repository.OutboxEventRepository;
import com.banking.transaction_service.service.OutboxEventService;
import com.banking.transaction_service.service.OutboxRetryService;
import com.banking.transaction_service.service.OutboxClaimService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxEventPublisher {

  private final OutboxEventRepository outboxEventRepository;
  private final OutboxClaimService outboxClaimService;
  private final OutboxEventService outboxEventService;
  private final OutboxRetryService outboxRetryService;

  private final KafkaTemplate<String, String> kafkaTemplate;


  public void publishPendingEvents() {

    List<OutboxEvent> events = outboxEventRepository
        .findFirst100ByPublishedFalseAndPermanentlyFailedFalseAndClaimedAtIsNullAndNextRetryAtIsNullOrderByCreatedAtAsc();

    if (events != null) {
      for (OutboxEvent event : events) {
        publishIfClaimed(event);
      }
    }

    List<OutboxEvent> retryEvents = outboxEventRepository
        .findFirst100ByPublishedFalseAndPermanentlyFailedFalseAndClaimedAtIsNullAndNextRetryAtLessThanEqualOrderByNextRetryAtAsc(
            LocalDateTime.now()
        );

    if (retryEvents != null) {
      for (OutboxEvent event : retryEvents) {
        publishIfClaimed(event);
      }
    }
  }

  private void publishIfClaimed(
      OutboxEvent event
  ) {

    boolean claimed =
        outboxClaimService.claim(
            event.getId()
        );

    if (!claimed) {

      log.debug(
          "Outbox event could not be claimed. eventId={}",
          event.getId()
      );

      return;
    }

    publishEvent(event);
  }


  private void publishEvent(
      OutboxEvent event
  ) {

    String topic;

    try {

      topic =
          resolveTopic(
              event.getEventType()
          );

      SendResult<String, String> result =
          kafkaTemplate
              .send(
                  topic,
                  event.getAggregateId().toString(),
                  event.getPayload()
              )
              .get(
                  10,
                  TimeUnit.SECONDS
              );

      RecordMetadata metadata =
          result.getRecordMetadata();

      log.info(
          "Outbox event published. " +
              "eventId={}, aggregateId={}, topic={}, partition={}, offset={}",
          event.getId(),
          event.getAggregateId(),
          metadata.topic(),
          metadata.partition(),
          metadata.offset()
      );

      boolean published =
          outboxEventService.markAsPublished(
              event.getId()
          );

      if (!published) {

        log.warn(
            "Kafka publish succeeded but outbox " +
                "could not be marked as published. eventId={}",
            event.getId()
        );
      }

    } catch (Exception exception) {

      log.error(
          "Outbox publish failed. eventId={}, retryCount={}",
          event.getId(),
          event.getRetryCount(),
          exception
      );

      outboxRetryService.handleFailure(
          event,
          exception
      );
    }
  }


  private String resolveTopic(
      String eventType
  ) {

    return switch (eventType) {

      case "TRANSACTION_REQUESTED" ->
          KafkaTopics.TRANSACTION_REQUESTED;

      case "TRANSACTION_COMPLETED" ->
          KafkaTopics.TRANSACTION_EVENTS;

      default ->
          throw new IllegalArgumentException(
              "Unknown outbox event type: "
                  + eventType
          );
    };
  }
}