package com.banking.transaction_service.service;

import com.banking.transaction_service.dto.request.TransactionCreateRequest;
import com.banking.transaction_service.entity.Transaction;
import com.banking.transaction_service.exception.IdempotencyKeyConflictException;
import com.banking.transaction_service.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransactionIdempotencyService {

  private final TransactionRepository transactionRepository;

  @Transactional(
      propagation = Propagation.REQUIRES_NEW,
      readOnly = true
  )
  public Transaction findExisting(
      String idempotencyKey
  ) {

    return transactionRepository
        .findByIdempotencyKey(
            idempotencyKey
        )
        .orElseThrow(() ->
            new IllegalStateException(
                "Transaction could not be found after "
                    + "idempotency conflict."
            )
        );
  }

  public void validate(
      Transaction existing,
      TransactionCreateRequest request
  ) {

    if (!existing.getSourceAccountId()
        .equals(
            request.getSourceAccountId()
        )) {

      throw new IdempotencyKeyConflictException(
          "Idempotency key was already used "
              + "with another source account."
      );
    }

    if (!existing.getDestinationAccountId()
        .equals(
            request.getDestinationAccountId()
        )) {

      throw new IdempotencyKeyConflictException(
          "Idempotency key was already used "
              + "with another destination account."
      );
    }

    if (existing.getAmount()
        .compareTo(
            request.getAmount()
        ) != 0) {

      throw new IdempotencyKeyConflictException(
          "Idempotency key was already used "
              + "with another amount."
      );
    }

    if (existing.getCurrency()
        != request.getCurrency()) {

      throw new IdempotencyKeyConflictException(
          "Idempotency key was already used "
              + "with another currency."
      );
    }

    if (existing.getTransactionType()
        != request.getTransactionType()) {

      throw new IdempotencyKeyConflictException(
          "Idempotency key was already used "
              + "with another transaction type."
      );
    }
  }
}