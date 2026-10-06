package com.banking.account_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import com.banking.account_service.entity.Account;
import com.banking.account_service.enums.AccountStatus;
import com.banking.account_service.enums.AccountType;
import com.banking.account_service.enums.Currency;
import com.banking.account_service.repository.AccountRepository;
import com.banking.account_service.dto.response.AccountResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AccountConcurrencyIntegrationTest {

  @Autowired
  private AccountService accountService;

  @Autowired
  private AccountRepository accountRepository;

  private UUID accountId;

  @BeforeEach
  void setUp() {

    Account account =
        Account.builder()
            .accountNumber("TR" + System.nanoTime())
            .customerId(UUID.randomUUID())
            .accountType(AccountType.CHECKING)
            .currency(Currency.TRY)
            .balance(new BigDecimal("1000.00"))
            .availableBalance(new BigDecimal("1000.00"))
            .status(AccountStatus.ACTIVE)
            .openedAt(LocalDateTime.now())
            .build();

    Account saved = accountRepository.saveAndFlush(account);

    accountId = saved.getId();
  }

  @Test
  void sameIdempotencyKeyShouldDebitOnlyOnce() throws Exception {

    String operationKey = "TX-CONCURRENT-DEBIT-001";

    int threadCount = 10;

    ExecutorService executor = Executors.newFixedThreadPool(threadCount);

    CountDownLatch ready = new CountDownLatch(threadCount);
    CountDownLatch start = new CountDownLatch(1);
    CountDownLatch done = new CountDownLatch(threadCount);

    ConcurrentLinkedQueue<Boolean> successResults = new ConcurrentLinkedQueue<>();

    BigDecimal amount = new BigDecimal("100.00");

    for (int i = 0; i < threadCount; i++) {

      executor.submit(() -> {

        try {

          ready.countDown();

          start.await();

          AccountResponse response =
              accountService.debit(
                  accountId,
                  amount,
                  operationKey
              );

          successResults.add(response != null);

        } catch (Exception exception) {

          successResults.add(false);

        } finally {

          done.countDown();
        }
      });
    }

    ready.await();

    start.countDown();

    done.await(15, TimeUnit.SECONDS);

    executor.shutdown();

    Account account =
        accountRepository
            .findById(accountId)
            .orElseThrow();

    assertThat(account.getBalance())
        .isEqualByComparingTo(new BigDecimal("900.00"));

    assertThat(account.getAvailableBalance())
        .isEqualByComparingTo(new BigDecimal("900.00"));
  }

  @Test
  void concurrentDifferentDebitsShouldNeverMakeBalanceNegative() throws Exception {

    Account account =
        accountRepository
            .findById(accountId)
            .orElseThrow();

    account.setBalance(new BigDecimal("500.00"));
    account.setAvailableBalance(new BigDecimal("500.00"));

    accountRepository.saveAndFlush(account);

    int threadCount = 10;

    ExecutorService executor = Executors.newFixedThreadPool(threadCount);

    CountDownLatch ready = new CountDownLatch(threadCount);
    CountDownLatch start = new CountDownLatch(1);
    CountDownLatch done = new CountDownLatch(threadCount);

    ConcurrentLinkedQueue<Boolean> results = new ConcurrentLinkedQueue<>();

    BigDecimal amount = new BigDecimal("100.00");

    for (int i = 0; i < threadCount; i++) {

      final int index = i;

      executor.submit(() -> {

        try {

          ready.countDown();

          start.await();

          // DÜZELTME: BalanceOperationRequest yerine doğrudan BigDecimal amount geçildi
          accountService.debit(
              accountId,
              amount,
              "CONCURRENT-" + index
          );

          results.add(true);

        } catch (Exception exception) {

          results.add(false);

        } finally {

          done.countDown();
        }
      });
    }

    ready.await();

    start.countDown();

    done.await(15, TimeUnit.SECONDS);

    executor.shutdown();

    Account finalAccount =
        accountRepository
            .findById(accountId)
            .orElseThrow();

    long successful =
        results.stream()
            .filter(Boolean::booleanValue)
            .count();

    assertThat(successful).isEqualTo(5);

    assertThat(finalAccount.getBalance())
        .isEqualByComparingTo(BigDecimal.ZERO);

    assertThat(finalAccount.getAvailableBalance())
        .isEqualByComparingTo(BigDecimal.ZERO);
  }
}