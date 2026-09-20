package com.banking.transaction_service.service;

import com.banking.transaction_service.dto.request.TransactionCreateRequest;
import com.banking.transaction_service.dto.response.TransactionResponse;
import java.util.List;
import java.util.UUID;

public interface TransactionService {

  TransactionResponse createTransaction(
      TransactionCreateRequest request
  );

  TransactionResponse getTransaction(
      UUID transactionId
  );

  TransactionResponse getByReference(
      String transactionReference
  );

  List<TransactionResponse> getAccountTransactions(
      UUID accountId
  );

  List<TransactionResponse> getAllTransactions();

}
