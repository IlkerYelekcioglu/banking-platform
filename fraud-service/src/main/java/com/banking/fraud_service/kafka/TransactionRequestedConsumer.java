package com.banking.fraud_service.kafka;

import com.banking.fraud_service.event.TransactionRequestedEvent;
import com.banking.fraud_service.service.FraudService;
import com.banking.fraud_service.service.SimpleInboxService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionRequestedConsumer {

  private final FraudService fraudService;
  private final SimpleInboxService simpleInboxService;
  private final ObjectMapper objectMapper;

  @KafkaListener(
      topics = "bankcore.transaction.requested",
      groupId = "fraud-service"
  )
  @Transactional
  public void consume(String message) {

    log.info(
        "Transaction requested event received. message={}",
        message
    );

    try {

      TransactionRequestedEvent event =
          objectMapper.readValue(
              message,
              TransactionRequestedEvent.class
          );

      boolean newEvent =
          simpleInboxService.markIfNew(
              event.getEventId(),
              "TRANSACTION_REQUESTED",
              event.getTransactionId(),
              "fraud-service"
          );

      if (!newEvent) {

        log.info(
            "Duplicate transaction requested event ignored. " +
                "eventId={}, transactionId={}",
            event.getEventId(),
            event.getTransactionId()
        );

        return;
      }

      fraudService.checkTransaction(event);

    } catch (Exception exception) {

      log.error(
          "Transaction requested event processing failed.",
          exception
      );

      throw new RuntimeException(
          "Fraud event processing failed.",
          exception
      );
    }
  }
}