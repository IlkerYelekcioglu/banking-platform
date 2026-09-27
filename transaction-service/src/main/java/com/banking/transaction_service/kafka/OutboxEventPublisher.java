package com.banking.transaction_service.kafka;

import com.banking.transaction_service.entity.OutboxEvent;
import com.banking.transaction_service.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxEventPublisher {

  private static final String TRANSACTION_TOPIC =
      "bankcore.transaction.events";

  private final OutboxEventRepository outboxEventRepository;

  private final KafkaTemplate<String, String> kafkaTemplate;

  @Scheduled(fixedDelay = 1000)
  @Transactional
  public void publishEvents() {

    List<OutboxEvent> events =
        outboxEventRepository
            .findTop100ByPublishedFalseOrderByCreatedAtAsc();

    for (OutboxEvent event : events) {

      try {

        kafkaTemplate.send(
            TRANSACTION_TOPIC,
            event.getAggregateId().toString(),
            event.getPayload()
        );

        event.setPublished(true);

        event.setPublishedAt(
            LocalDateTime.now()
        );

        outboxEventRepository.save(event);

        log.info(
            "Outbox event published. eventId={}, eventType={}",
            event.getId(),
            event.getEventType()
        );

      } catch (Exception exception) {

        log.error(
            "Failed to publish outbox event. eventId={}",
            event.getId(),
            exception
        );
      }
    }
  }
}