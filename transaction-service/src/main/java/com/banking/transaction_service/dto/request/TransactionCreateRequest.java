package com.banking.transaction_service.dto.request;

import com.banking.transaction_service.enums.Currency;
import com.banking.transaction_service.enums.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Setter;
import lombok.Getter;

@Getter
@Setter
public class TransactionCreateRequest {

  private UUID eventId;

  @NotNull
  private UUID sourceAccountId;

  @NotNull
  private UUID destinationAccountId;

  @NotNull
  @DecimalMin(
      value = "0.01",
      message = "Amount must be greater than zero"
  )
  private BigDecimal amount;

  @NotNull
  private Currency currency;

  @NotNull
  private TransactionType transactionType;

  private String description;

  private String channel;

  private String ipAddress;

  private String deviceId;

  private String location;

  @NotNull
  private String idempotencyKey;


}
