package com.banking.transaction_service.dto.response;


import com.banking.transaction_service.enums.Currency;
import com.banking.transaction_service.enums.TransactionStatus;
import com.banking.transaction_service.enums.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Builder
@Setter
public class TransactionResponse {
  private UUID id;

  private String transactionReference;

  private UUID sourceAccountId;

  private UUID destinationAccountId;

  private BigDecimal amount;

  private Currency currency;

  private TransactionType transactionType;

  private TransactionStatus status;

  private String description;

  private String channel;

  private LocalDateTime transactionDate;

  private BigDecimal fraudScore;

  private String failureReason;

  private boolean debitCompleted;

  private boolean creditCompleted;

  private boolean compensationCompleted;

  private LocalDateTime createdAt;

  private LocalDateTime updatedAt;

}
