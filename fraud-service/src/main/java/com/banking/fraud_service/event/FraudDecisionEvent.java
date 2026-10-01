package com.banking.fraud_service.event;
import com.banking.fraud_service.enums.FraudDecision;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudDecisionEvent {

  private UUID eventId;

  private UUID transactionId;

  private String transactionReference;

  private BigDecimal fraudScore;

  private FraudDecision decision;

  private String reason;
}