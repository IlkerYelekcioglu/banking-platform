package com.banking.transaction_service.event;

import com.banking.transaction_service.enums.Currency;
import com.banking.transaction_service.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRequestedEvent {

  private UUID transactionId;

  private String transactionReference;

  private UUID sourceAccountId;

  private UUID destinationAccountId;

  private BigDecimal amount;

  private Currency currency;

  private TransactionType transactionType;

  private String channel;

  private String ipAddress;

  private String deviceId;

  private String location;

  private LocalDateTime transactionDate;

  private UUID eventId;
}