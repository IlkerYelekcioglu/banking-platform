package com.banking.transaction_service.kafka;

import com.banking.transaction_service.event.FraudDecisionEvent;
import com.banking.transaction_service.service.SimpleInboxService;
import com.banking.transaction_service.service.SimpleTransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
@Slf4j
public class FraudDecisionConsumer {

  private final SimpleTransactionService transactionService;

  private final ObjectMapper objectMapper;

  private final SimpleInboxService inboxService;

  @KafkaListener(
      topics = "bankcore.fraud.decisions",
      groupId = "transaction-service"
  )
  @Transactional
  public void consume(String message) {

    log.info(
        "Fraud decision received. message={}",
        message
    );

    try {

      FraudDecisionEvent event =
          objectMapper.readValue(
              message,
              FraudDecisionEvent.class
          );

      boolean newEvent =
          inboxService.markIfNew(
              event.getEventId(),
              "FRAUD_DECISION",
              event.getTransactionId(),
              "transaction-service"
          );

      if (!newEvent) {

        log.info(
            "Duplicate fraud decision event ignored. " +
                "eventId={}, transactionId={}",
            event.getEventId(),
            event.getTransactionId()
        );

        return;
      }

      transactionService.processFraudDecision(event);

    } catch (Exception exception) {

      log.error(
          "Fraud decision processing failed.",
          exception
      );

      throw new RuntimeException(
          "Fraud decision processing failed.",
          exception
      );
    }
  }
}