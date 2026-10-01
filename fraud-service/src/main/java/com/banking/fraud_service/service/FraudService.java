package com.banking.fraud_service.service;

import com.banking.fraud_service.entity.FraudCheck;
import com.banking.fraud_service.enums.FraudDecision;
import com.banking.fraud_service.event.FraudDecisionEvent;
import com.banking.fraud_service.event.TransactionRequestedEvent;
import com.banking.fraud_service.repository.FraudCheckRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class FraudService {

  private static final String FRAUD_DECISION_TOPIC =
      "bankcore.fraud.decisions";

  private final RedisTemplate<String, String> redisTemplate;

  private final FraudCheckRepository fraudCheckRepository;

  private final KafkaTemplate<String, String> kafkaTemplate;

  @Transactional
  public void checkTransaction(
      TransactionRequestedEvent event) {

    if (fraudCheckRepository
        .findByTransactionId(event.getTransactionId())
        .isPresent()) {

      return;
    }

    BigDecimal fraudScore =
        calculateFraudScore(event);

    FraudDecision decision;

    String reason;

    if (fraudScore.compareTo(
        BigDecimal.valueOf(70)) >= 0) {

      decision = FraudDecision.BLOCKED;

      reason = "Transaction exceeded fraud risk threshold.";

    } else {

      decision = FraudDecision.APPROVED;

      reason = "Transaction passed fraud checks.";
    }

    FraudCheck fraudCheck =
        FraudCheck.builder()
            .transactionId(
                event.getTransactionId()
            )
            .transactionReference(
                event.getTransactionReference()
            )
            .fraudScore(fraudScore)
            .decision(decision)
            .reason(reason)
            .checkedAt(LocalDateTime.now())
            .build();

    fraudCheckRepository.save(fraudCheck);

    publishDecision(
        event,
        fraudScore,
        decision,
        reason
    );
  }

  private BigDecimal calculateFraudScore(
      TransactionRequestedEvent event) {

    int score = 0;

    BigDecimal amount = event.getAmount();

    if (amount.compareTo(
        BigDecimal.valueOf(10_000)) > 0) {

      score += 30;
    }

    if (amount.compareTo(
        BigDecimal.valueOf(50_000)) > 0) {

      score += 30;
    }

    if (event.getIpAddress() != null) {

      Long ipRequestCount =
          increaseCounter(
              "fraud:ip:"
                  + event.getIpAddress()
          );

      if (ipRequestCount > 5) {
        score += 20;
      }
    }

    if (event.getDeviceId() != null) {

      Long deviceRequestCount =
          increaseCounter(
              "fraud:device:"
                  + event.getDeviceId()
          );

      if (deviceRequestCount > 5) {
        score += 20;
      }
    }

    if (score > 100) {
      score = 100;
    }

    return BigDecimal.valueOf(score);
  }

  private Long increaseCounter(String key) {

    Long count =
        redisTemplate
            .opsForValue()
            .increment(key);

    redisTemplate.expire(
        key,
        Duration.ofMinutes(5)
    );

    return count;
  }

  private void publishDecision(
      TransactionRequestedEvent event,
      BigDecimal fraudScore,
      FraudDecision decision,
      String reason) {

    FraudDecisionEvent decisionEvent =
        FraudDecisionEvent.builder()
            .transactionId(event.getTransactionId())
            .transactionReference(event.getTransactionReference())
            .fraudScore(fraudScore)
            .decision(decision)
            .reason(reason)
            .build();

    try {

      String payload =
          new com.fasterxml.jackson.databind.ObjectMapper()
              .writeValueAsString(
                  decisionEvent
              );

      kafkaTemplate.send(
          FRAUD_DECISION_TOPIC,
          event.getTransactionId().toString(),
          payload
      );

    } catch (Exception exception) {

      throw new RuntimeException(
          "Failed to publish fraud decision.",
          exception
      );
    }
  }
}