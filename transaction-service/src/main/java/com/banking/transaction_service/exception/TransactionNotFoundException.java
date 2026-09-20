package com.banking.transaction_service.exception;

import java.util.UUID;

public class TransactionNotFoundException extends RuntimeException {

  public TransactionNotFoundException(UUID transactionId) {
    super("Transaction not found with id: " + transactionId);
  }

  public TransactionNotFoundException(String transactionReference) {
    super(
        "Transaction not found with reference: "
            + transactionReference
    );
  }
}