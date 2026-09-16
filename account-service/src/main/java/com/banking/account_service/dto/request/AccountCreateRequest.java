package com.banking.account_service.dto.request;

import com.banking.account_service.enums.AccountType;
import com.banking.account_service.enums.Currency;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccountCreateRequest {

  @NotNull
  private UUID customerId;

  @NotNull
  private AccountType accountType;

  @NotNull
  private Currency currency;

}
