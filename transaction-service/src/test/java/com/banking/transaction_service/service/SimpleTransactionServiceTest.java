package com.banking.transaction_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.banking.transaction_service.client.AccountClient;
import com.banking.transaction_service.dto.request.BalanceOperationRequest;
import com.banking.transaction_service.dto.request.TransactionCreateRequest;
import com.banking.transaction_service.dto.response.AccountResponse;
import com.banking.transaction_service.entity.Transaction;
import com.banking.transaction_service.enums.Currency;
import com.banking.transaction_service.enums.TransactionStatus;
import com.banking.transaction_service.enums.TransactionType;
import com.banking.transaction_service.exception.TransactionProcessingException;
import com.banking.transaction_service.mapper.TransactionMapper;
import com.banking.transaction_service.repository.TransactionRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.banking.transaction_service.dto.response.TransactionResponse;
import com.banking.transaction_service.exception.InvalidTransactionException;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;


@ExtendWith(MockitoExtension.class)
class SimpleTransactionServiceTest {

  @Mock
  private TransactionRepository transactionRepository;

  @Mock
  private TransactionMapper transactionMapper;

  @Mock
  private AccountClient accountClient;

  @InjectMocks
  private SimpleTransactionService transactionService;

  private UUID sourceAccountId;
  private UUID destinationAccountId;

  @BeforeEach
  void setUp() {
    sourceAccountId = UUID.randomUUID();
    destinationAccountId = UUID.randomUUID();
  }

  // ---------------------------------------------------------
  // SUCCESSFUL TRANSFER
  // ---------------------------------------------------------

  @Test
  void createTransaction_shouldCompleteTransferSuccessfully() {

    TransactionCreateRequest request =
        createRequest();

    AccountResponse sourceAccount =
        createAccount(
            sourceAccountId,
            new BigDecimal("1000.00")
        );

    AccountResponse destinationAccount =
        createAccount(
            destinationAccountId,
            new BigDecimal("500.00")
        );

    Transaction pendingTransaction =
        createTransaction(
            TransactionStatus.PENDING
        );

    Transaction completedTransaction =
        createTransaction(
            TransactionStatus.COMPLETED
        );

    TransactionResponse response =
        TransactionResponse.builder()
            .id(pendingTransaction.getId())
            .transactionReference(
                pendingTransaction
                    .getTransactionReference()
            )
            .sourceAccountId(sourceAccountId)
            .destinationAccountId(destinationAccountId)
            .amount(new BigDecimal("300.00"))
            .currency(Currency.TRY)
            .transactionType(TransactionType.TRANSFER)
            .status(TransactionStatus.COMPLETED)
            .build();

    when(transactionRepository
        .findByIdempotencyKey(
            request.getIdempotencyKey()
        ))
        .thenReturn(Optional.empty());

    when(accountClient.getAccount(sourceAccountId))
        .thenReturn(sourceAccount);

    when(accountClient.getAccount(destinationAccountId))
        .thenReturn(destinationAccount);

    when(transactionRepository.save(any(Transaction.class)))
        .thenReturn(pendingTransaction)
        .thenReturn(completedTransaction);

    when(accountClient.debit(
        eq(sourceAccountId),
        any(BalanceOperationRequest.class)
    )).thenReturn(sourceAccount);

    when(accountClient.credit(
        eq(destinationAccountId),
        any(BalanceOperationRequest.class)
    )).thenReturn(destinationAccount);

    when(transactionMapper.toResponse(
        completedTransaction
    )).thenReturn(response);

    TransactionResponse result =
        transactionService.createTransaction(request);

    assertNotNull(result);

    assertEquals(
        TransactionStatus.COMPLETED,
        result.getStatus()
    );

    verify(accountClient)
        .debit(
            eq(sourceAccountId),
            any(BalanceOperationRequest.class)
        );

    verify(accountClient)
        .credit(
            eq(destinationAccountId),
            any(BalanceOperationRequest.class)
        );

    verify(transactionRepository, times(2))
        .save(any(Transaction.class));
  }

  // ---------------------------------------------------------
  // SAME ACCOUNT
  // ---------------------------------------------------------

  @Test
  void createTransaction_shouldRejectSameSourceAndDestination() {

    TransactionCreateRequest request =
        createRequest();

    request.setDestinationAccountId(
        request.getSourceAccountId()
    );

    assertThrows(
        InvalidTransactionException.class,
        () -> transactionService
            .createTransaction(request)
    );

    verify(accountClient, never())
        .getAccount(any(UUID.class));

    verify(transactionRepository, never())
        .save(any(Transaction.class));
  }

  // ---------------------------------------------------------
  // IDEMPOTENCY
  // ---------------------------------------------------------

  @Test
  void createTransaction_shouldReturnExistingTransaction_whenIdempotencyKeyExists() {

    Transaction existingTransaction =
        createTransaction(
            TransactionStatus.COMPLETED
        );

    TransactionResponse response =
        TransactionResponse.builder()
            .id(existingTransaction.getId())
            .transactionReference(
                existingTransaction
                    .getTransactionReference()
            )
            .status(TransactionStatus.COMPLETED)
            .build();

    TransactionCreateRequest request =
        createRequest();

    when(transactionRepository
        .findByIdempotencyKey(
            request.getIdempotencyKey()
        ))
        .thenReturn(Optional.of(existingTransaction));

    when(transactionMapper.toResponse(
        existingTransaction
    )).thenReturn(response);

    TransactionResponse result =
        transactionService.createTransaction(request);

    assertNotNull(result);

    assertEquals(
        TransactionStatus.COMPLETED,
        result.getStatus()
    );

    verify(accountClient, never())
        .debit(any(UUID.class), any());

    verify(accountClient, never())
        .credit(any(UUID.class), any());

    verify(transactionRepository, never())
        .save(any(Transaction.class));
  }

  // ---------------------------------------------------------
  // CURRENCY MISMATCH
  // ---------------------------------------------------------

  @Test
  void createTransaction_shouldRejectCurrencyMismatch() {

    TransactionCreateRequest request =
        createRequest();

    request.setCurrency(Currency.USD);

    AccountResponse sourceAccount =
        createAccount(
            sourceAccountId,
            new BigDecimal("1000.00")
        );

    AccountResponse destinationAccount =
        createAccount(
            destinationAccountId,
            new BigDecimal("500.00")
        );

    when(transactionRepository
        .findByIdempotencyKey(
            request.getIdempotencyKey()
        ))
        .thenReturn(Optional.empty());

    when(accountClient.getAccount(sourceAccountId))
        .thenReturn(sourceAccount);

    when(accountClient.getAccount(destinationAccountId))
        .thenReturn(destinationAccount);

    assertThrows(
        InvalidTransactionException.class,
        () -> transactionService
            .createTransaction(request)
    );

    verify(accountClient, never())
        .debit(any(UUID.class), any());

    verify(accountClient, never())
        .credit(any(UUID.class), any());
  }

  // ---------------------------------------------------------
  // DEBIT FAILURE
  // ---------------------------------------------------------

  @Test
  void createTransaction_shouldMarkFailed_whenDebitFails() {

    TransactionCreateRequest request =
        createRequest();

    AccountResponse sourceAccount =
        createAccount(
            sourceAccountId,
            new BigDecimal("1000.00")
        );

    AccountResponse destinationAccount =
        createAccount(
            destinationAccountId,
            new BigDecimal("500.00")
        );

    Transaction transaction =
        createTransaction(
            TransactionStatus.PENDING
        );

    when(transactionRepository
        .findByIdempotencyKey(
            request.getIdempotencyKey()
        ))
        .thenReturn(Optional.empty());

    when(accountClient.getAccount(sourceAccountId))
        .thenReturn(sourceAccount);

    when(accountClient.getAccount(destinationAccountId))
        .thenReturn(destinationAccount);

    when(transactionRepository.save(any(Transaction.class)))
        .thenReturn(transaction);

    when(accountClient.debit(
        eq(sourceAccountId),
        any(BalanceOperationRequest.class)
    )).thenThrow(
        new RuntimeException("Debit failed")
    );

    assertThrows(
        TransactionProcessingException.class,
        () -> transactionService
            .createTransaction(request)
    );

    assertEquals(
        TransactionStatus.FAILED,
        transaction.getStatus()
    );

    verify(accountClient, never())
        .credit(
            eq(destinationAccountId),
            any(BalanceOperationRequest.class)
        );
  }

  // ---------------------------------------------------------
  // CREDIT FAILURE + COMPENSATION
  // ---------------------------------------------------------

  @Test
  void createTransaction_shouldCompensateDebit_whenCreditFails() {

    TransactionCreateRequest request =
        createRequest();

    AccountResponse sourceAccount =
        createAccount(
            sourceAccountId,
            new BigDecimal("1000.00")
        );

    AccountResponse destinationAccount =
        createAccount(
            destinationAccountId,
            new BigDecimal("500.00")
        );

    Transaction transaction =
        createTransaction(
            TransactionStatus.PENDING
        );

    when(transactionRepository
        .findByIdempotencyKey(
            request.getIdempotencyKey()
        ))
        .thenReturn(Optional.empty());

    when(accountClient.getAccount(sourceAccountId))
        .thenReturn(sourceAccount);

    when(accountClient.getAccount(destinationAccountId))
        .thenReturn(destinationAccount);

    when(transactionRepository.save(any(Transaction.class)))
        .thenReturn(transaction);

    when(accountClient.debit(
        eq(sourceAccountId),
        any(BalanceOperationRequest.class)
    )).thenReturn(sourceAccount);

    when(accountClient.credit(
        eq(destinationAccountId),
        any(BalanceOperationRequest.class)
    )).thenThrow(
        new RuntimeException("Credit failed")
    );

    when(accountClient.credit(
        eq(sourceAccountId),
        any(BalanceOperationRequest.class)
    )).thenReturn(sourceAccount);

    assertThrows(
        TransactionProcessingException.class,
        () -> transactionService
            .createTransaction(request)
    );

    assertEquals(
        TransactionStatus.FAILED,
        transaction.getStatus()
    );

    verify(accountClient)
        .debit(
            eq(sourceAccountId),
            any(BalanceOperationRequest.class)
        );

    verify(accountClient)
        .credit(
            eq(destinationAccountId),
            any(BalanceOperationRequest.class)
        );

    verify(accountClient)
        .credit(
            eq(sourceAccountId),
            any(BalanceOperationRequest.class)
        );
  }

  // ---------------------------------------------------------
  // HELPERS
  // ---------------------------------------------------------

  private TransactionCreateRequest createRequest() {

    TransactionCreateRequest request =
        new TransactionCreateRequest();

    request.setSourceAccountId(sourceAccountId);
    request.setDestinationAccountId(
        destinationAccountId
    );
    request.setAmount(
        new BigDecimal("300.00")
    );
    request.setCurrency(Currency.TRY);
    request.setTransactionType(
        TransactionType.TRANSFER
    );
    request.setDescription("Test transfer");
    request.setChannel("MOBILE");
    request.setIpAddress("127.0.0.1");
    request.setDeviceId("TEST-DEVICE");
    request.setLocation("Istanbul");
    request.setIdempotencyKey(
        "TEST-" + UUID.randomUUID()
    );

    return request;
  }

  private AccountResponse createAccount(
      UUID accountId,
      BigDecimal balance) {

    return AccountResponse.builder()
        .id(accountId)
        .accountNumber("TR123456789")
        .customerId(UUID.randomUUID())
        .accountType("CHECKING")
        .currency(Currency.TRY)
        .balance(balance)
        .availableBalance(balance)
        .status("ACTIVE")
        .build();
  }

  private Transaction createTransaction(
      TransactionStatus status) {

    return Transaction.builder()
        .transactionReference(
            "TXN-" + UUID.randomUUID()
        )
        .sourceAccountId(sourceAccountId)
        .destinationAccountId(
            destinationAccountId
        )
        .amount(new BigDecimal("300.00"))
        .currency(Currency.TRY)
        .transactionType(
            TransactionType.TRANSFER
        )
        .status(status)
        .description("Test transfer")
        .channel("MOBILE")
        .transactionDate(
            java.time.LocalDateTime.now()
        )
        .idempotencyKey(
            "TEST-" + UUID.randomUUID()
        )
        .build();
  }
}