package com.banking.transaction_service.service;


import com.banking.transaction_service.client.AccountClient;
import com.banking.transaction_service.dto.request.TransactionCreateRequest;
import com.banking.transaction_service.dto.response.AccountResponse;
import com.banking.transaction_service.dto.response.TransactionResponse;
import com.banking.transaction_service.entity.Transaction;
import com.banking.transaction_service.enums.TransactionStatus;
import com.banking.transaction_service.exception.InsufficientBalanceException;
import com.banking.transaction_service.exception.TransactionNotFoundException;
import com.banking.transaction_service.exception.InvalidTransactionException;
import com.banking.transaction_service.mapper.TransactionMapper;
import com.banking.transaction_service.repository.TransactionRepository;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SimpleTransactionService
    implements TransactionService {

  private final TransactionRepository transactionRepository;
  private final TransactionMapper transactionMapper;
  private final AccountClient accountClient;

  @Override
  @Transactional
  public TransactionResponse createTransaction(
      TransactionCreateRequest request) {

    /*
     * 1. Idempotency kontrolü
     *
     * Aynı transaction isteği daha önce gönderilmişse
     * yeni transaction oluşturma.
     */
    Transaction existingTransaction =
        transactionRepository
            .findByIdempotencyKey(
                request.getIdempotencyKey()
            )
            .orElse(null);

    if (existingTransaction != null) {

      return transactionMapper.toResponse(
          existingTransaction
      );
    }

    /*
     * 2. Source ve destination aynı hesap mı?
     */
    if (request.getSourceAccountId()
        .equals(request.getDestinationAccountId())) {

      throw new InvalidTransactionException(
          "Source account and destination account "
              + "cannot be the same."
      );
    }

    /*
     * 3. Amount kontrolü
     */
    if (request.getAmount() == null
        || request.getAmount()
        .compareTo(BigDecimal.ZERO) <= 0) {

      throw new InvalidTransactionException(
          "Transaction amount must be greater than zero."
      );
    }

    /*
     * 4. Source Account'u Account Service'ten getir.
     *
     * Transaction Service account_db'ye
     * direkt erişmez.
     */
    AccountResponse sourceAccount =
        getAccount(
            request.getSourceAccountId()
        );

    /*
     * 5. Destination Account'u getir.
     */
    AccountResponse destinationAccount =
        getAccount(
            request.getDestinationAccountId()
        );

    /*
     * 6. Source Account aktif mi?
     */
    validateAccountIsActive(
        sourceAccount,
        "Source account"
    );

    /*
     * 7. Destination Account aktif mi?
     */
    validateAccountIsActive(
        destinationAccount,
        "Destination account"
    );

    /*
     * 8. Source currency kontrolü
     */
    if (sourceAccount.getCurrency()
        != request.getCurrency()) {

      throw new InvalidTransactionException(
          "Transaction currency "
              + request.getCurrency()
              + " does not match source account currency "
              + sourceAccount.getCurrency()
      );
    }

    /*
     * 9. Destination currency kontrolü
     */
    if (destinationAccount.getCurrency()
        != request.getCurrency()) {

      throw new InvalidTransactionException(
          "Transaction currency "
              + request.getCurrency()
              + " does not match destination account currency "
              + destinationAccount.getCurrency()
      );
    }

    /*
     * 10. Available Balance kontrolü
     */
    validateBalance(
        sourceAccount,
        request.getAmount()
    );

    /*
     * 11. Transaction Reference oluştur.
     */
    String transactionReference =
        generateTransactionReference();

    /*
     * 12. Transaction entity oluştur.
     */
    Transaction transaction =
        Transaction.builder()
            .transactionReference(
                transactionReference
            )
            .sourceAccountId(
                request.getSourceAccountId()
            )
            .destinationAccountId(
                request.getDestinationAccountId()
            )
            .amount(
                request.getAmount()
            )
            .currency(
                request.getCurrency()
            )
            .transactionType(
                request.getTransactionType()
            )
            .status(
                TransactionStatus.PENDING
            )
            .description(
                request.getDescription()
            )
            .channel(
                request.getChannel()
            )
            .ipAddress(
                request.getIpAddress()
            )
            .deviceId(
                request.getDeviceId()
            )
            .location(
                request.getLocation()
            )
            .transactionDate(
                LocalDateTime.now()
            )
            .idempotencyKey(
                request.getIdempotencyKey()
            )
            .build();

    /*
     * 13. Database'e kaydet.
     */
    Transaction savedTransaction =
        transactionRepository.save(transaction);

    /*
     * 14. Response döndür.
     */
    return transactionMapper.toResponse(
        savedTransaction
    );
  }

  /*
   * Account Service üzerinden account getirir.
   */
  private AccountResponse getAccount(
      UUID accountId) {

    try {

      return accountClient.getAccount(accountId);

    } catch (Exception exception) {

      throw new InvalidTransactionException(
          "Account could not be retrieved: "
              + accountId
      );
    }
  }

  /*
   * Account ACTIVE mi?
   */
  private void validateAccountIsActive(
      AccountResponse account,
      String accountDescription) {

    if (account == null) {

      throw new InvalidTransactionException(
          accountDescription
              + " could not be found."
      );
    }

    if (!"ACTIVE".equalsIgnoreCase(
        account.getStatus()
    )) {

      throw new InvalidTransactionException(
          accountDescription
              + " is not active. Current status: "
              + account.getStatus()
      );
    }
  }

  /*
   * Source account yeterli bakiyeye sahip mi?
   */
  private void validateBalance(
      AccountResponse sourceAccount,
      BigDecimal amount) {

    BigDecimal availableBalance =
        sourceAccount.getAvailableBalance();

    if (availableBalance == null) {

      throw new InvalidTransactionException(
          "Source account balance information "
              + "is not available."
      );
    }

    if (availableBalance.compareTo(amount) < 0) {

      throw new InsufficientBalanceException(
          amount,
          availableBalance
      );
    }
  }

  @Override
  public List<TransactionResponse> getAllTransactions() {

    return transactionRepository.findAll()
        .stream()
        .map(transactionMapper::toResponse)
        .toList();
  }

  /*
   * Transaction reference oluşturur.
   *
   * Örnek:
   * TXN-1758191234567-A12BC345
   */
  private String generateTransactionReference() {

    return "TXN-"
        + System.currentTimeMillis()
        + "-"
        + UUID.randomUUID()
        .toString()
        .substring(0, 8)
        .toUpperCase();
  }

  @Override
  public TransactionResponse getTransaction(
      UUID transactionId) {

    Transaction transaction =
        transactionRepository
            .findById(transactionId)
            .orElseThrow(
                () -> new TransactionNotFoundException(
                    transactionId
                )
            );

    return transactionMapper.toResponse(
        transaction
    );
  }

  @Override
  public TransactionResponse getByReference(
      String transactionReference) {

    Transaction transaction =
        transactionRepository
            .findByTransactionReference(
                transactionReference
            )
            .orElseThrow(
                () -> new TransactionNotFoundException(
                    transactionReference
                )
            );

    return transactionMapper.toResponse(
        transaction
    );
  }

  @Override
  public List<TransactionResponse>
  getAccountTransactions(UUID accountId) {

    List<Transaction> outgoingTransactions =
        transactionRepository
            .findBySourceAccountId(accountId);

    List<Transaction> incomingTransactions =
        transactionRepository
            .findByDestinationAccountId(accountId);

    return java.util.stream.Stream
        .concat(
            outgoingTransactions.stream(),
            incomingTransactions.stream()
        )
        .map(transactionMapper::toResponse)
        .toList();
  }
}