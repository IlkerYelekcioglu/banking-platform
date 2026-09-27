package com.banking.fraud_service.kafka;

import com.banking.fraud_service.event.TransactionRequestedEvent;
import com.banking.fraud_service.service.FraudService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionRequestedConsumer {

  private final FraudService fraudService;

  private final ObjectMapper objectMapper;

  @KafkaListener(
      topics = "bankcore.transaction.requested",
      groupId = "fraud-service"
  )
  public void consume(String message) {

    try {

      TransactionRequestedEvent event =
          objectMapper.readValue(
              message,
              TransactionRequestedEvent.class
          );

      log.info(
          "Transaction requested event received. transactionId={}",
          event.getTransactionId()
      );

      fraudService.checkTransaction(event);

    } catch (Exception exception) {

      log.error(
          "Failed to process transaction requested event.",
          exception
      );

      throw new RuntimeException(
          "Fraud event processing failed.",
          exception
      );
    }
  }
}