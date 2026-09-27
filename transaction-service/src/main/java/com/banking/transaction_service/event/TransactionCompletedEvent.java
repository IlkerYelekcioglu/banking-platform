package com.banking.transaction_service.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionCompletedEvent {

  private UUID transactionId;

  private String transactionReference;

  private UUID sourceAccountId;

  private UUID destinationAccountId;

  private BigDecimal amount;

  private String currency;

  private String transactionType;

  private LocalDateTime transactionDate;
}