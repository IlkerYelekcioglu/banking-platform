package com.banking.account_service.dto.response;

import com.banking.account_service.enums.AccountStatus;
import com.banking.account_service.enums.AccountType;
import com.banking.account_service.enums.Currency;
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
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountResponse {


  private UUID id;

  private String accountNumber;

  private UUID customerId;

  private AccountType accountType;

  private Currency currency;

  private BigDecimal balance;

  private BigDecimal availableBalance;

  private AccountStatus status;

  private LocalDateTime openedAt;

  private LocalDateTime createdAt;

  private LocalDateTime updatedAt;

}
