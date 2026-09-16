package com.banking.account_service.mapper;

import com.banking.account_service.dto.response.AccountResponse;
import com.banking.account_service.entity.Account;
import org.springframework.stereotype.Component;

@Component
public class AccountMapper {

  public AccountResponse toResponse(Account account) {

    return AccountResponse.builder()
        .id(account.getId())
        .accountNumber(account.getAccountNumber())
        .customerId(account.getCustomerId())
        .accountType(account.getAccountType())
        .currency(account.getCurrency())
        .balance(account.getBalance())
        .availableBalance(account.getAvailableBalance())
        .status(account.getStatus())
        .openedAt(account.getOpenedAt())
        .createdAt(account.getCreatedAt())
        .updatedAt(account.getUpdatedAt())
        .build();
  }
}