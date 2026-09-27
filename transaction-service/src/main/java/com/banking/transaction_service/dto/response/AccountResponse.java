package com.banking.transaction_service.dto.response;

import com.banking.transaction_service.enums.Currency;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AccountResponse {
  private UUID id;

  private String accountNumber;

  private UUID customerId;

  private String accountType;

  private Currency currency;

  private BigDecimal balance;

  private BigDecimal availableBalance;

  private String status;

}
