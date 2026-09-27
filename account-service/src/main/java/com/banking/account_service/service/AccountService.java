package com.banking.account_service.service;

import com.banking.account_service.dto.request.AccountCreateRequest;
import com.banking.account_service.dto.response.AccountBalanceResponse;
import com.banking.account_service.dto.response.AccountResponse;
import com.banking.account_service.enums.AccountStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface AccountService {

  AccountResponse createAccount(
      AccountCreateRequest request
  );

  AccountResponse getAccount(
      UUID accountId
  );

  List<AccountResponse> getCustomerAccounts(
      UUID customerId
  );

  AccountBalanceResponse getBalance(
      UUID accountId
  );

  AccountResponse changeStatus(
      UUID accountId,
      AccountStatus status
  );

  AccountResponse closeAccount(
      UUID accountId
  );

  List<AccountResponse> getAllAccounts();

  AccountResponse debit(
      UUID accountId,
      BigDecimal amount
  );

  AccountResponse credit(
      UUID accountId,
      BigDecimal amount
  );

}