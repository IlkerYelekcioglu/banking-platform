package com.banking.account_service.repository;

import com.banking.account_service.entity.Account;
import com.banking.account_service.enums.AccountStatus;
import feign.Param;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface AccountRepository extends JpaRepository<Account, UUID> {

  Optional<Account> findByAccountNumber(String accountNumber);

  List<Account> findByCustomerId(UUID customerId);

  List<Account> findByCustomerIdAndStatus(
      UUID customerId,
      AccountStatus status
  );

  boolean existsByAccountNumber(String accountNumber);

  @Modifying(
      clearAutomatically = true,
      flushAutomatically = true
  )
  @Query("""
        UPDATE Account a
        SET
            a.balance = a.balance - :amount,
            a.availableBalance = a.availableBalance - :amount,
            a.version = a.version + 1
        WHERE
            a.id = :accountId
            AND a.status = :status
            AND a.availableBalance >= :amount
    """)
  int debitIfSufficientBalance(
      @Param("accountId") UUID accountId,
      @Param("amount") BigDecimal amount,
      @Param("status") AccountStatus status
  );

  @Modifying(
      clearAutomatically = true,
      flushAutomatically = true
  )
  @Query("""
        UPDATE Account a
        SET
            a.balance = a.balance + :amount,
            a.availableBalance = a.availableBalance + :amount,
            a.version = a.version + 1
        WHERE
            a.id = :accountId
            AND a.status = :status
    """)
  int creditIfActive(
      @Param("accountId") UUID accountId,
      @Param("amount") BigDecimal amount,
      @Param("status") AccountStatus status
  );
}
