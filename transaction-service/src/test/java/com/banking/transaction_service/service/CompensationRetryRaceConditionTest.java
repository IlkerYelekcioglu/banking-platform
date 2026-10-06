package com.banking.transaction_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.banking.transaction_service.client.AccountClient;
import com.banking.transaction_service.config.CompensationProperties;
import com.banking.transaction_service.entity.Transaction;
import com.banking.transaction_service.enums.Currency;
import com.banking.transaction_service.enums.TransactionStatus;
import com.banking.transaction_service.enums.TransactionType;
import com.banking.transaction_service.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CompensationRetryRaceConditionTest {

  @Mock
  private TransactionRepository transactionRepository;

  @Mock
  private AccountClient accountClient;

  @Mock
  private SimpleCompensationClaimService compensationClaimService;

  @Mock
  private CompensationProperties compensationProperties;

  @Mock
  private CompensationRetryPolicy retryPolicy;

  @InjectMocks
  private SimpleCompensationService compensationService;

  @Test
  void retryWithWrongClaimTokenShouldNotCallAccountService() {

    UUID transactionId =
        UUID.randomUUID();

    String validToken =
        "VALID-TOKEN";

    String wrongToken =
        "WRONG-TOKEN";

    Transaction transaction =
        createCompensatingTransaction(
            transactionId,
            validToken
        );

    when(
        transactionRepository.findById(
            transactionId
        )
    ).thenReturn(
        java.util.Optional.of(transaction)
    );

    compensationService.retryCompensation(
        transactionId,
        wrongToken
    );

    verify(
        accountClient,
        never()
    ).credit(
        any(),
        any(),
        any()
    );

    verify(
        transactionRepository,
        never()
    ).save(
        any()
    );
  }

  @Test
  void retryWithValidClaimTokenShouldCallAccountService() {

    UUID transactionId =
        UUID.randomUUID();

    String validToken =
        "VALID-TOKEN";

    Transaction transaction =
        createCompensatingTransaction(
            transactionId,
            validToken
        );

    when(
        transactionRepository.findById(
            transactionId
        )
    ).thenReturn(
        java.util.Optional.of(transaction)
    );

    compensationService.retryCompensation(
        transactionId,
        validToken
    );

    verify(
        accountClient,
        times(1)
    ).credit(
        eq(transaction.getSourceAccountId()),
        any(),
        eq(
            "TX-"
                + transactionId
                + "-COMPENSATION"
        )
    );

    assertThat(
        transaction.isCompensationCompleted()
    ).isTrue();

    assertThat(
        transaction.getStatus()
    ).isEqualTo(
        TransactionStatus.FAILED
    );
  }

  private Transaction createCompensatingTransaction(
      UUID transactionId,
      String claimToken) {

    return Transaction.builder()
        .id(transactionId)
        .transactionReference(
            "TX-" + transactionId
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
            TransactionStatus.COMPENSATING
        )
        .transactionDate(
            LocalDateTime.now()
        )
        .idempotencyKey(
            UUID.randomUUID().toString()
        )
        .debitCompleted(true)
        .creditCompleted(false)
        .compensationCompleted(false)
        .compensationClaimToken(
            claimToken
        )
        .compensationRetryCount(0)
        .build();
  }
}