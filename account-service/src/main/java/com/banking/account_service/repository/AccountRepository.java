package com.banking.account_service.repository;

import com.banking.account_service.entity.Account;
import com.banking.account_service.enums.AccountStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, UUID> {

  Optional<Account> findByAccountNumber(String accountNumber);

  List<Account> findByCustomerId(UUID customerId);

  List<Account> findByCustomerIdAndStatus(
      UUID customerId,
      AccountStatus status
  );

  boolean existsByAccountNumber(String accountNumber);

}
