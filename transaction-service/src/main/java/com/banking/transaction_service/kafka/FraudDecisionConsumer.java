package com.banking.transaction_service.kafka;

import com.banking.transaction_service.event.FraudDecisionEvent;
import com.banking.transaction_service.service.SimpleTransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
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

  @KafkaListener(
      topics = "bankcore.fraud.decisions",
      groupId = "transaction-service"
  )
  public void consume(String message) {

    try {

      log.info(
          "Fraud decision event received. message={}",
          message
      );

      FraudDecisionEvent event =
          objectMapper.readValue(
              message,
              FraudDecisionEvent.class
          );

      transactionService.processFraudDecision(
          event
      );

    } catch (Exception exception) {

      log.error(
          "Failed to process fraud decision event.",
          exception
      );

      throw new RuntimeException(
          "Fraud decision processing failed.",
          exception
      );
    }
  }
}
