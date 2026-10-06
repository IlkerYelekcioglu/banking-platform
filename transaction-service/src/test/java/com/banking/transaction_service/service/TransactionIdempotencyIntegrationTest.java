package com.banking.transaction_service.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.banking.transaction_service.dto.request.TransactionCreateRequest;
import com.banking.transaction_service.dto.response.TransactionResponse;
import com.banking.transaction_service.enums.Currency;
import com.banking.transaction_service.enums.TransactionType;
import com.banking.transaction_service.repository.OutboxEventRepository;
import com.banking.transaction_service.repository.TransactionRepository;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TransactionIdempotencyIntegrationTest {

  @Autowired
  private TransactionService transactionService;

  @Autowired
  private TransactionRepository transactionRepository;

  @Autowired
  private OutboxEventRepository outboxEventRepository;

  @Test
  void sameIdempotencyKeyShouldCreateOnlyOneTransaction()
      throws Exception {

    String idempotencyKey =
        "TX-IDEMPOTENCY-001";

    UUID sourceAccount =
        UUID.randomUUID();

    UUID destinationAccount =
        UUID.randomUUID();

    TransactionCreateRequest request =
        createRequest(
            sourceAccount,
            destinationAccount,
            new BigDecimal("100.00"),
            idempotencyKey
        );

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

    ConcurrentLinkedQueue<TransactionResponse>
        responses =
        new ConcurrentLinkedQueue<>();

    for (int i = 0; i < threadCount; i++) {

      executor.submit(() -> {

        try {

          ready.countDown();

          start.await();

          TransactionResponse response =
              transactionService
                  .createTransaction(
                      request
                  );

          responses.add(response);

        } catch (Exception exception) {

        } finally {

          done.countDown();
        }
      });
    }

    ready.await();

    start.countDown();

    done.await(
        15,
        TimeUnit.SECONDS
    );

    executor.shutdown();

    long transactionCount =
        transactionRepository
            .findAll()
            .stream()
            .filter(transaction ->
                idempotencyKey.equals(
                    transaction
                        .getIdempotencyKey()
                )
            )
            .count();

    assertThat(transactionCount)
        .isEqualTo(1);

    long outboxCount =
        outboxEventRepository
            .findAll()
            .stream()
            .filter(event ->
                "TRANSACTION_REQUESTED"
                    .equals(
                        event.getEventType()
                    )
            )
            .filter(event ->
                responses.stream()
                    .anyMatch(response ->
                        response.getId()
                            .equals(
                                event.getAggregateId()
                            )
                    )
            )
            .count();

    assertThat(outboxCount)
        .isEqualTo(1);
  }

  private TransactionCreateRequest createRequest(
      UUID sourceAccount,
      UUID destinationAccount,
      BigDecimal amount,
      String idempotencyKey
  ) {

    TransactionCreateRequest request =
        new TransactionCreateRequest();

    request.setSourceAccountId(
        sourceAccount
    );

    request.setDestinationAccountId(
        destinationAccount
    );

    request.setAmount(amount);

    request.setCurrency(
        Currency.TRY
    );

    request.setTransactionType(
        TransactionType.TRANSFER
    );

    request.setDescription(
        "Concurrency test"
    );

    request.setChannel(
        "TEST"
    );

    request.setIdempotencyKey(
        idempotencyKey
    );

    return request;
  }

  @Test
  void sameIdempotencyKeyWithDifferentAmountShouldReturnConflict() {

    String idempotencyKey =
        "TX-CONFLICT-001";

    UUID source =
        UUID.randomUUID();

    UUID destination =
        UUID.randomUUID();

    TransactionCreateRequest first =
        createRequest(
            source,
            destination,
            new BigDecimal("100.00"),
            idempotencyKey
        );

    transactionService.createTransaction(
        first
    );

    TransactionCreateRequest second =
        createRequest(
            source,
            destination,
            new BigDecimal("500.00"),
            idempotencyKey
        );

    org.assertj.core.api.Assertions
        .assertThatThrownBy(() ->
            transactionService
                .createTransaction(second)
        )
        .hasMessageContaining(
            "another amount"
        );
  }
}