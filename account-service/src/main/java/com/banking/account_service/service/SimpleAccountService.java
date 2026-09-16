package com.banking.account_service.service;

import com.banking.account_service.client.CustomerClient;
import com.banking.account_service.dto.response.AccountBalanceResponse;
import com.banking.account_service.dto.request.AccountCreateRequest;
import com.banking.account_service.dto.response.AccountResponse;
import com.banking.account_service.entity.Account;
import com.banking.account_service.enums.AccountStatus;
import com.banking.account_service.exception.AccountNotFoundException;
import com.banking.account_service.exception.DuplicateAccountException;
import com.banking.account_service.mapper.AccountMapper;
import com.banking.account_service.repository.AccountRepository;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SimpleAccountService implements AccountService {

  private final AccountRepository accountRepository;
  private final AccountMapper accountMapper;
  private final CustomerClient customerClient;

  @Override
  @Transactional
  public AccountResponse createAccount(AccountCreateRequest request) {

    customerClient.getCustomer(request.getCustomerId());

    String accountNumber = generateAccountNumber();

    if (accountRepository.existsByAccountNumber(accountNumber)) {
      throw new DuplicateAccountException(accountNumber);
    }

    Account account = Account.builder()
        .accountNumber(accountNumber)
        .customerId(request.getCustomerId())
        .accountType(request.getAccountType())
        .currency(request.getCurrency())
        .balance(BigDecimal.ZERO)
        .availableBalance(BigDecimal.ZERO)
        .status(AccountStatus.ACTIVE)
        .openedAt(java.time.LocalDateTime.now())
        .build();

    Account savedAccount = accountRepository.save(account);

    return accountMapper.toResponse(savedAccount);
  }

  @Override
  public AccountResponse getAccount(UUID accountId) {

    Account account = findAccountById(accountId);

    return accountMapper.toResponse(account);
  }

  @Override
  public List<AccountResponse> getCustomerAccounts(UUID customerId) {

    return accountRepository.findByCustomerId(customerId)
        .stream()
        .map(accountMapper::toResponse)
        .collect(Collectors.toList());
  }

  @Override
  public AccountBalanceResponse getBalance(UUID accountId) {

    Account account = findAccountById(accountId);

    return AccountBalanceResponse.builder()
        .accountId(account.getId())
        .balance(account.getBalance())
        .availableBalance(account.getAvailableBalance())
        .currency(account.getCurrency())
        .build();
  }

  @Override
  @Transactional
  public AccountResponse changeStatus(
      UUID accountId,
      AccountStatus status) {

    Account account = findAccountById(accountId);

    if (account.getStatus() == AccountStatus.CLOSED
        && status != AccountStatus.CLOSED) {

      throw new IllegalStateException(
          "Closed account status cannot be changed."
      );
    }

    account.setStatus(status);

    Account updatedAccount = accountRepository.save(account);

    return accountMapper.toResponse(updatedAccount);
  }

  @Override
  @Transactional
  public AccountResponse closeAccount(UUID accountId) {

    Account account = findAccountById(accountId);

    if (account.getStatus() == AccountStatus.CLOSED) {
      throw new IllegalStateException(
          "Account is already closed."
      );
    }

    account.setStatus(AccountStatus.CLOSED);
    account.setClosedAt(java.time.LocalDateTime.now());

    Account closedAccount = accountRepository.save(account);

    return accountMapper.toResponse(closedAccount);
  }

  private Account findAccountById(UUID accountId) {

    return accountRepository.findById(accountId)
        .orElseThrow(
            () -> new AccountNotFoundException(accountId)
        );
  }

  private String generateAccountNumber() {

    return "TR"
        + System.currentTimeMillis()
        + String.format(
        "%04d",
        new java.util.Random().nextInt(10000)
    );
  }
}