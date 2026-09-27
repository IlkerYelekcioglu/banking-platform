package com.banking.account_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.banking.account_service.client.CustomerClient;
import com.banking.account_service.dto.request.AccountCreateRequest;
import com.banking.account_service.dto.response.AccountResponse;
import com.banking.account_service.dto.response.CustomerResponse;
import com.banking.account_service.entity.Account;
import com.banking.account_service.enums.AccountStatus;
import com.banking.account_service.enums.AccountType;
import com.banking.account_service.enums.Currency;
import com.banking.account_service.exception.AccountNotFoundException;
import com.banking.account_service.exception.InsufficientBalanceException;
import com.banking.account_service.mapper.AccountMapper;
import com.banking.account_service.repository.AccountRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
public class SimpleAccountServiceTest {

  @Mock
  private AccountRepository accountRepository;

  @Mock
  private AccountMapper accountMapper;

  @Mock
  private CustomerClient customerClient;

  @InjectMocks
  private SimpleAccountService accountService;

  private UUID accountId;
  private UUID customerId;
  private Account account;
  private AccountResponse accountResponse;


  @BeforeEach
  void setUp() {

    accountId =
        UUID.randomUUID();

    account =
        Account.builder()
            .accountNumber("TR123456789")
            .customerId(UUID.randomUUID())
            .accountType(AccountType.CHECKING)
            .currency(Currency.TRY)
            .balance(new BigDecimal("10000.00"))
            .availableBalance(new BigDecimal("10000.00"))
            .status(AccountStatus.ACTIVE)
            .build();

    accountResponse =
        AccountResponse.builder()
            .accountNumber("TR123456789")
            .customerId(account.getCustomerId())
            .accountType(AccountType.CHECKING)
            .currency(Currency.TRY)
            .balance(new BigDecimal("8000.00"))
            .availableBalance(new BigDecimal("8000.00"))
            .status(AccountStatus.ACTIVE)
            .build();
  }

  @Test
  void shouldDebitSuccessfully() {

    when(accountRepository.findById(accountId))
        .thenReturn(Optional.of(account));

    when(
        accountRepository.debitIfSufficientBalance(
            eq(accountId),
            eq(new BigDecimal("2000.00")),
            eq(AccountStatus.ACTIVE)
        )
    ).thenReturn(1);

    Account updatedAccount =
        Account.builder()
            .accountNumber("TR123456789")
            .customerId(account.getCustomerId())
            .accountType(AccountType.CHECKING)
            .currency(Currency.TRY)
            .balance(new BigDecimal("8000.00"))
            .availableBalance(new BigDecimal("8000.00"))
            .status(AccountStatus.ACTIVE)
            .build();

    when(accountRepository.findById(accountId))
        .thenReturn(
            Optional.of(account),
            Optional.of(updatedAccount)
        );

    when(accountMapper.toResponse(updatedAccount))
        .thenReturn(accountResponse);

    AccountResponse result =
        accountService.debit(
            accountId,
            new BigDecimal("2000.00")
        );

    assertNotNull(result);

    assertEquals(
        new BigDecimal("8000.00"),
        result.getBalance()
    );

    verify(accountRepository)
        .debitIfSufficientBalance(
            accountId,
            new BigDecimal("2000.00"),
            AccountStatus.ACTIVE
        );
  }

  // ---------------------------------------------------------
  // DEBIT
  // ---------------------------------------------------------

  @Test
  void debit_shouldDecreaseBalance_whenBalanceIsSufficient() {

    Account account = createAccount(
        new BigDecimal("1000.00")
    );

    AccountResponse response = AccountResponse.builder()
        .id(accountId)
        .balance(new BigDecimal("700.00"))
        .availableBalance(new BigDecimal("700.00"))
        .currency(Currency.TRY)
        .status(AccountStatus.ACTIVE)
        .build();

    when(accountRepository.findById(accountId))
        .thenReturn(Optional.of(account));

    when(accountRepository.save(any(Account.class)))
        .thenReturn(account);

    when(accountMapper.toResponse(account))
        .thenReturn(response);

    AccountResponse result =
        accountService.debit(
            accountId,
            new BigDecimal("300.00")
        );

    assertNotNull(result);

    assertEquals(
        new BigDecimal("700.00"),
        account.getBalance()
    );

    assertEquals(
        new BigDecimal("700.00"),
        account.getAvailableBalance()
    );

    verify(accountRepository).save(account);
  }

  @Test
  void shouldNotDebitBlockedAccount() {

    account.setStatus(AccountStatus.BLOCKED);

    when(accountRepository.findById(accountId))
        .thenReturn(Optional.of(account));

    assertThrows(
        IllegalStateException.class,
        () ->
            accountService.debit(
                accountId,
                new BigDecimal("1000.00")
            )
    );

    verify(
        accountRepository,
        never()
    ).debitIfSufficientBalance(
        any(),
        any(),
        any()
    );
  }

  @Test
  void shouldRejectZeroAmount() {

    assertThrows(
        IllegalArgumentException.class,
        () ->
            accountService.debit(
                accountId,
                BigDecimal.ZERO
            )
    );

    verifyNoInteractions(accountRepository);
  }

  @Test
  void shouldCreditSuccessfully() {

    when(accountRepository.findById(accountId))
        .thenReturn(Optional.of(account));

    when(
        accountRepository.creditIfActive(
            eq(accountId),
            eq(new BigDecimal("3000.00")),
            eq(AccountStatus.ACTIVE)
        )
    ).thenReturn(1);

    Account updatedAccount =
        Account.builder()
            .accountNumber("TR123456789")
            .customerId(account.getCustomerId())
            .accountType(AccountType.CHECKING)
            .currency(Currency.TRY)
            .balance(new BigDecimal("13000.00"))
            .availableBalance(new BigDecimal("13000.00"))
            .status(AccountStatus.ACTIVE)
            .build();

    when(accountRepository.findById(accountId))
        .thenReturn(
            Optional.of(account),
            Optional.of(updatedAccount)
        );

    AccountResponse response =
        AccountResponse.builder()
            .accountNumber("TR123456789")
            .balance(new BigDecimal("13000.00"))
            .availableBalance(new BigDecimal("13000.00"))
            .status(AccountStatus.ACTIVE)
            .currency(Currency.TRY)
            .build();

    when(accountMapper.toResponse(updatedAccount))
        .thenReturn(response);

    AccountResponse result =
        accountService.credit(
            accountId,
            new BigDecimal("3000.00")
        );

    assertNotNull(result);

    assertEquals(
        new BigDecimal("13000.00"),
        result.getBalance()
    );

    verify(accountRepository)
        .creditIfActive(
            accountId,
            new BigDecimal("3000.00"),
            AccountStatus.ACTIVE
        );
  }
  @Test
  void shouldThrowInsufficientBalanceException() {

    when(accountRepository.findById(accountId))
        .thenReturn(Optional.of(account));

    when(
        accountRepository.debitIfSufficientBalance(
            eq(accountId),
            eq(new BigDecimal("15000.00")),
            eq(AccountStatus.ACTIVE)
        )
    ).thenReturn(0);

    when(accountRepository.findById(accountId))
        .thenReturn(
            Optional.of(account),
            Optional.of(account)
        );

    assertThrows(
        InsufficientBalanceException.class,
        () ->
            accountService.debit(
                accountId,
                new BigDecimal("15000.00")
            )
    );
  }

  @Test
  void debit_shouldThrowException_whenBalanceIsInsufficient() {

    Account account = createAccount(
        new BigDecimal("100.00")
    );

    when(accountRepository.findById(accountId))
        .thenReturn(Optional.of(account));

    assertThrows(
        InsufficientBalanceException.class,
        () -> accountService.debit(
            accountId,
            new BigDecimal("500.00")
        )
    );

    verify(accountRepository, never())
        .save(any(Account.class));
  }

  @Test
  void debit_shouldThrowException_whenAccountDoesNotExist() {

    when(accountRepository.findById(accountId))
        .thenReturn(Optional.empty());

    assertThrows(
        AccountNotFoundException.class,
        () -> accountService.debit(
            accountId,
            new BigDecimal("100.00")
        )
    );

    verify(accountRepository, never())
        .save(any(Account.class));
  }

  @Test
  void debit_shouldThrowException_whenAccountIsBlocked() {

    Account account = createAccount(
        new BigDecimal("1000.00")
    );

    account.setStatus(AccountStatus.BLOCKED);

    when(accountRepository.findById(accountId))
        .thenReturn(Optional.of(account));

    assertThrows(
        IllegalStateException.class,
        () -> accountService.debit(
            accountId,
            new BigDecimal("100.00")
        )
    );

    verify(accountRepository, never())
        .save(any(Account.class));
  }

  // ---------------------------------------------------------
  // CREDIT
  // ---------------------------------------------------------

  @Test
  void credit_shouldIncreaseBalance_whenAccountIsActive() {

    Account account = createAccount(
        new BigDecimal("1000.00")
    );

    AccountResponse response = AccountResponse.builder()
        .id(accountId)
        .balance(new BigDecimal("1500.00"))
        .availableBalance(new BigDecimal("1500.00"))
        .currency(Currency.TRY)
        .status(AccountStatus.ACTIVE)
        .build();

    when(accountRepository.findById(accountId))
        .thenReturn(Optional.of(account));

    when(accountRepository.save(any(Account.class)))
        .thenReturn(account);

    when(accountMapper.toResponse(account))
        .thenReturn(response);

    AccountResponse result =
        accountService.credit(
            accountId,
            new BigDecimal("500.00")
        );

    assertNotNull(result);

    assertEquals(
        new BigDecimal("1500.00"),
        account.getBalance()
    );

    assertEquals(
        new BigDecimal("1500.00"),
        account.getAvailableBalance()
    );

    verify(accountRepository).save(account);
  }

  @Test
  void credit_shouldThrowException_whenAccountIsBlocked() {

    Account account = createAccount(
        new BigDecimal("1000.00")
    );

    account.setStatus(AccountStatus.BLOCKED);

    when(accountRepository.findById(accountId))
        .thenReturn(Optional.of(account));

    assertThrows(
        IllegalStateException.class,
        () -> accountService.credit(
            accountId,
            new BigDecimal("500.00")
        )
    );

    verify(accountRepository, never())
        .save(any(Account.class));
  }

  @Test
  void credit_shouldThrowException_whenAmountIsZero() {

    assertThrows(
        IllegalArgumentException.class,
        () -> accountService.credit(
            accountId,
            BigDecimal.ZERO
        )
    );

    verify(accountRepository, never())
        .findById(any());
  }

  // ---------------------------------------------------------
  // CREATE ACCOUNT
  // ---------------------------------------------------------

  @Test
  void createAccount_shouldCreateActiveAccount() {

    AccountCreateRequest request =
        new AccountCreateRequest();

    request.setCustomerId(customerId);
    request.setAccountType(AccountType.CHECKING);
    request.setCurrency(Currency.TRY);

    CustomerResponse customerResponse =
        new CustomerResponse();

    customerResponse.setId(customerId);

    when(customerClient.getCustomer(customerId))
        .thenReturn(customerResponse);

    when(accountRepository.existsByAccountNumber(anyString()))
        .thenReturn(false);

    Account savedAccount =
        createAccount(BigDecimal.ZERO);

    when(accountRepository.save(any(Account.class)))
        .thenReturn(savedAccount);

    AccountResponse response =
        AccountResponse.builder()
            .id(accountId)
            .customerId(customerId)
            .accountType(AccountType.CHECKING)
            .currency(Currency.TRY)
            .balance(BigDecimal.ZERO)
            .availableBalance(BigDecimal.ZERO)
            .status(AccountStatus.ACTIVE)
            .build();

    when(accountMapper.toResponse(savedAccount))
        .thenReturn(response);

    AccountResponse result =
        accountService.createAccount(request);

    assertNotNull(result);

    assertEquals(
        AccountStatus.ACTIVE,
        result.getStatus()
    );

    assertEquals(
        BigDecimal.ZERO,
        result.getBalance()
    );

    verify(customerClient)
        .getCustomer(customerId);

    verify(accountRepository)
        .save(any(Account.class));
  }

  // ---------------------------------------------------------
  // HELPER
  // ---------------------------------------------------------

  private Account createAccount(BigDecimal balance) {

    return Account.builder()
        .accountNumber("TR000000000000000000000000")
        .customerId(customerId)
        .accountType(AccountType.CHECKING)
        .currency(Currency.TRY)
        .balance(balance)
        .availableBalance(balance)
        .status(AccountStatus.ACTIVE)
        .openedAt(java.time.LocalDateTime.now())
        .build();
  }
}
