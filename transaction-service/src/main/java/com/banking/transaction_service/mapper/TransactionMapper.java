package com.banking.transaction_service.mapper;

import com.banking.transaction_service.dto.response.TransactionResponse;
import com.banking.transaction_service.entity.Transaction;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

  public TransactionResponse toResponse(
      Transaction transaction) {

    return TransactionResponse.builder()
        .id(transaction.getId())
        .transactionReference(
            transaction.getTransactionReference()
        )
        .sourceAccountId(
            transaction.getSourceAccountId()
        )
        .destinationAccountId(
            transaction.getDestinationAccountId()
        )
        .amount(transaction.getAmount())
        .currency(transaction.getCurrency())
        .transactionType(
            transaction.getTransactionType()
        )
        .status(transaction.getStatus())
        .description(transaction.getDescription())
        .channel(transaction.getChannel())
        .transactionDate(
            transaction.getTransactionDate()
        )
        .fraudScore(
            transaction.getFraudScore()
        )
        .failureReason(
            transaction.getFailureReason()
        )
        .debitCompleted(
            transaction.isDebitCompleted()
        )
        .creditCompleted(
            transaction.isCreditCompleted()
        )
        .compensationCompleted(
            transaction.isCompensationCompleted()
        )
        .createdAt(transaction.getCreatedAt())
        .updatedAt(transaction.getUpdatedAt())
        .build();
  }
}