package com.banking.account_service.repository;

import com.banking.account_service.entity.BalanceOperation;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BalanceOperationRepository
    extends JpaRepository<BalanceOperation, UUID> {

  Optional<BalanceOperation>
  findByOperationKey(
      String operationKey
  );

  boolean existsByOperationKey(
      String operationKey
  );

  @Modifying
  @Query(
      value = """
                    INSERT INTO balance_operations
                    (
                        id,
                        operation_key,
                        account_id,
                        amount,
                        operation_type,
                        status,
                        created_at
                    )
                    VALUES
                    (
                        gen_random_uuid(),
                        :operationKey,
                        :accountId,
                        :amount,
                        :operationType,
                        'PROCESSING',
                        CURRENT_TIMESTAMP
                    )
                    ON CONFLICT (operation_key)
                    DO NOTHING
                    """,
      nativeQuery = true
  )
  int tryCreateOperation(@Param("operationKey") String operationKey, @Param("accountId") UUID accountId, @Param("amount") BigDecimal amount, @Param("operationType") String operationType);

  @Modifying
  @Query("""
            UPDATE BalanceOperation b
            SET
                b.status = com.banking.account_service.enums.BalanceOperationStatus.COMPLETED,
                b.balanceAfter = :balanceAfter,
                b.availableBalanceAfter = :availableBalanceAfter,
                b.completedAt = CURRENT_TIMESTAMP
            WHERE
                b.operationKey = :operationKey
                AND b.status =
                    com.banking.account_service.enums.BalanceOperationStatus.PROCESSING
            """)
  int markAsCompleted(@Param("operationKey") String operationKey, @Param("balanceAfter") BigDecimal balanceAfter, @Param("availableBalanceAfter") BigDecimal availableBalanceAfter);

}
