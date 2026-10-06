package com.banking.account_service.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.banking.account_service.entity.Account;
import com.banking.account_service.enums.AccountStatus;
import com.banking.account_service.enums.AccountType;
import com.banking.account_service.enums.BalanceOperationType;
import com.banking.account_service.enums.Currency;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
@AutoConfigureTestDatabase(
    replace = AutoConfigureTestDatabase.Replace.NONE
)
class BalanceOperationConcurrencyTest {

  @Autowired
  private AccountRepository accountRepository;

  @Autowired
  private BalanceOperationRepository balanceOperationRepository;

  private UUID accountId;

  @BeforeEach
  void setUp() {

    Account account =
        Account.builder()
            .accountNumber(
                "TR000000000000000000000001"
            )
            .customerId(UUID.randomUUID())
            .accountType(AccountType.CHECKING)
            .currency(Currency.TRY)
            .balance(
                new BigDecimal("1000.00")
            )
            .availableBalance(
                new BigDecimal("1000.00")
            )
            .status(AccountStatus.ACTIVE)
            .openedAt(
                java.time.LocalDateTime.now()
            )
            .build();

    Account saved =
        accountRepository.saveAndFlush(account);

    accountId = saved.getId();
  }

  @Test
  void sameOperationKeyShouldCreateOnlyOneOperation()
      throws Exception {

    String operationKey =
        "TX-CONCURRENT-001";

    int threadCount = 10;

    ExecutorService executor =
        Executors.newFixedThreadPool(
            threadCount
        );

    CountDownLatch ready =
        new CountDownLatch(threadCount);

    CountDownLatch start =
        new CountDownLatch(1);

    CountDownLatch done =
        new CountDownLatch(threadCount);

    ConcurrentLinkedQueue<Integer> results =
        new ConcurrentLinkedQueue<>();

    for (int i = 0; i < threadCount; i++) {

      executor.submit(() -> {

        try {

          ready.countDown();

          start.await();

          int result =
              balanceOperationRepository
                  .tryCreateOperation(
                      operationKey,
                      accountId,
                      new BigDecimal("100.00"),
                      BalanceOperationType.DEBIT.name()
                  );

          results.add(result);

        } catch (Exception exception) {

          throw new RuntimeException(
              exception
          );

        } finally {

          done.countDown();
        }
      });
    }

    ready.await();

    start.countDown();

    done.await(
        10,
        TimeUnit.SECONDS
    );

    executor.shutdown();

    long successfulInserts =
        results.stream()
            .filter(result -> result == 1)
            .count();

    assertThat(successfulInserts)
        .isEqualTo(1);

    assertThat(
        balanceOperationRepository
            .findByOperationKey(
                operationKey
            )
    )
        .isPresent();
  }
}