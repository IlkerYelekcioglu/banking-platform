package com.banking.transaction_service.service;


import static org.assertj.core.api.Assertions.assertThat;

import com.banking.transaction_service.entity.Transaction;
import com.banking.transaction_service.enums.Currency;
import com.banking.transaction_service.enums.TransactionStatus;
import com.banking.transaction_service.enums.TransactionType;
import com.banking.transaction_service.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CompensationRaceConditionIntegrationTest {

  @Autowired
  private SimpleCompensationClaimService compensationClaimService;

  @Autowired
  private TransactionRepository transactionRepository;

  private ExecutorService executor;

  @BeforeEach
  void setUp() {
    executor = Executors.newFixedThreadPool(2);
  }

  @AfterEach
  void tearDown() {
    executor.shutdownNow();
  }

  @Test
  void onlyOneWorkerShouldClaimSameCompensation()
      throws Exception {

    Transaction transaction =
        createPendingCompensation();

    CountDownLatch ready =
        new CountDownLatch(2);

    CountDownLatch start =
        new CountDownLatch(1);

    CountDownLatch done =
        new CountDownLatch(2);

    ConcurrentLinkedQueue<String> claimResults =
        new ConcurrentLinkedQueue<>();

    for (int i = 0; i < 2; i++) {

      executor.submit(() -> {

        try {

          ready.countDown();

          start.await();

          String claimToken =
              compensationClaimService.claim(
                  transaction.getId()
              );

          claimResults.add(
              claimToken
          );

        } catch (Exception exception) {

          throw new RuntimeException(
              exception
          );

        } finally {

          done.countDown();
        }
      });
    }

    assertThat(
        ready.await(
            5,
            TimeUnit.SECONDS
        )
    ).isTrue();

    start.countDown();

    assertThat(
        done.await(
            10,
            TimeUnit.SECONDS
        )
    ).isTrue();

    List<String> successfulClaims =
        claimResults.stream()
            .filter(token -> token != null)
            .toList();

    assertThat(successfulClaims)
        .hasSize(1);

    long failedClaims =
        claimResults.stream()
            .filter(token -> token == null)
            .count();

    assertThat(failedClaims)
        .isEqualTo(1);

    Transaction databaseTransaction =
        transactionRepository
            .findById(transaction.getId())
            .orElseThrow();

    assertThat(
        databaseTransaction.getStatus()
    ).isEqualTo(
        TransactionStatus.COMPENSATING
    );

    assertThat(
        databaseTransaction
            .getCompensationClaimToken()
    ).isNotBlank();

    assertThat(
        databaseTransaction
            .getCompensationClaimToken()
    ).isEqualTo(
        successfulClaims.get(0)
    );
  }

  @Test
  void secondClaimShouldReturnNullAfterFirstWorkerClaims()
      throws Exception {

    Transaction transaction =
        createPendingCompensation();

    String firstClaim =
        compensationClaimService.claim(
            transaction.getId()
        );

    String secondClaim =
        compensationClaimService.claim(
            transaction.getId()
        );

    assertThat(firstClaim)
        .isNotBlank();

    assertThat(secondClaim)
        .isNull();

    Transaction databaseTransaction =
        transactionRepository
            .findById(transaction.getId())
            .orElseThrow();

    assertThat(
        databaseTransaction.getStatus()
    ).isEqualTo(
        TransactionStatus.COMPENSATING
    );

    assertThat(
        databaseTransaction
            .getCompensationClaimToken()
    ).isEqualTo(firstClaim);
  }

  @Test
  void differentTransactionsCanBeClaimedConcurrently()
      throws Exception {

    Transaction firstTransaction =
        createPendingCompensation();

    Transaction secondTransaction =
        createPendingCompensation();

    CountDownLatch ready =
        new CountDownLatch(2);

    CountDownLatch start =
        new CountDownLatch(1);

    CountDownLatch done =
        new CountDownLatch(2);

    ConcurrentLinkedQueue<String> results =
        new ConcurrentLinkedQueue<>();

    executor.submit(() -> {

      try {

        ready.countDown();
        start.await();

        results.add(
            compensationClaimService.claim(
                firstTransaction.getId()
            )
        );

      } catch (Exception exception) {

        throw new RuntimeException(
            exception
        );

      } finally {

        done.countDown();
      }
    });

    executor.submit(() -> {

      try {

        ready.countDown();
        start.await();

        results.add(
            compensationClaimService.claim(
                secondTransaction.getId()
            )
        );

      } catch (Exception exception) {

        throw new RuntimeException(
            exception
        );

      } finally {

        done.countDown();
      }
    });

    assertThat(
        ready.await(
            5,
            TimeUnit.SECONDS
        )
    ).isTrue();

    start.countDown();

    assertThat(
        done.await(
            10,
            TimeUnit.SECONDS
        )
    ).isTrue();

    assertThat(results)
        .hasSize(2);

    assertThat(
        results.stream()
            .filter(token -> token != null)
            .count()
    ).isEqualTo(2);

    Transaction first =
        transactionRepository
            .findById(
                firstTransaction.getId()
            )
            .orElseThrow();

    Transaction second =
        transactionRepository
            .findById(
                secondTransaction.getId()
            )
            .orElseThrow();

    assertThat(first.getStatus())
        .isEqualTo(
            TransactionStatus.COMPENSATING
        );

    assertThat(second.getStatus())
        .isEqualTo(
            TransactionStatus.COMPENSATING
        );
  }

  @Test
  void claimShouldNotBePossibleWhenTransactionIsAlreadyCompensating() {

    Transaction transaction =
        createPendingCompensation();

    String firstClaim =
        compensationClaimService.claim(
            transaction.getId()
        );

    assertThat(firstClaim)
        .isNotBlank();

    String secondClaim =
        compensationClaimService.claim(
            transaction.getId()
        );

    assertThat(secondClaim)
        .isNull();
  }

  private Transaction createPendingCompensation() {

    Transaction transaction =
        Transaction.builder()
            .transactionReference(
                "TX-" +
                    UUID.randomUUID()
            )
            .sourceAccountId(
                UUID.randomUUID()
            )
            .destinationAccountId(
                UUID.randomUUID()
            )
            .amount(
                new BigDecimal("100.00")
            )
            .currency(
                Currency.TRY
            )
            .transactionType(
                TransactionType.TRANSFER
            )
            .status(
                TransactionStatus.COMPENSATION_REQUIRED
            )
            .transactionDate(
                LocalDateTime.now()
            )
            .idempotencyKey(
                "IDEMP-" +
                    UUID.randomUUID()
            )
            .debitCompleted(true)
            .creditCompleted(false)
            .compensationCompleted(false)
            .compensationRetryCount(0)
            .nextCompensationRetryAt(
                LocalDateTime.now()
            )
            .build();

    return transactionRepository.saveAndFlush(
        transaction
    );
  }
}