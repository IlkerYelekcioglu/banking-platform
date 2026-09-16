package com.banking.account_service.dto.response;

import com.banking.account_service.enums.Currency;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;



@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountBalanceResponse {

  private UUID accountId;

  private String accountNumber;

  private BigDecimal balance;

  private BigDecimal availableBalance;

  private Currency currency;

}
