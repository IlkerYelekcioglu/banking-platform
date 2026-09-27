package com.banking.fraud_service.repository;

import com.banking.fraud_service.entity.FraudCheck;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FraudCheckRepository extends JpaRepository<FraudCheck, UUID> {

  Optional<FraudCheck> findByTransactionId(UUID transactionId);
}
