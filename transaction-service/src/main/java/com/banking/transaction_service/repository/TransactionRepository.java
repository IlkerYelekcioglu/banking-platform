package com.banking.transaction_service.repository;

import com.banking.transaction_service.entity.Transaction;
import com.banking.transaction_service.enums.TransactionStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

  Optional<Transaction> findByTransactionReference(
      String transactionReference
  );

  Optional<Transaction> findByIdempotencyKey(
      String idempotencyKey
  );

  List<Transaction> findBySourceAccountId(
      UUID sourceAccountId
  );

  List<Transaction> findByDestinationAccountId(
      UUID destinationAccountId
  );

  List<Transaction> findBySourceAccountIdAndStatus(
      UUID sourceAccountId,
      TransactionStatus status
  );

  boolean existsByIdempotencyKey(
      String idempotencyKey
  );


}
