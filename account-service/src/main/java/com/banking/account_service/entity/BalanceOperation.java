package com.banking.account_service.entity;

import com.banking.account_service.enums.BalanceOperationStatus;
import com.banking.account_service.enums.BalanceOperationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "balance_operations",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_balance_operation_key",
            columnNames = "operation_key"
        )
    },
    indexes = {
        @Index(
            name = "idx_balance_operation_account",
            columnList = "account_id"
        )
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BalanceOperation {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(
      name = "operation_key",
      nullable = false,
      unique = true,
      length = 150
  )
  private String operationKey;

  @Column(
      name = "account_id",
      nullable = false
  )
  private UUID accountId;

  @Column(
      nullable = false,
      precision = 19,
      scale = 4
  )
  private BigDecimal amount;

  @Enumerated(EnumType.STRING)
  @Column(
      name = "operation_type",
      nullable = false,
      length = 30
  )
  private BalanceOperationType operationType;

  @Enumerated(EnumType.STRING)
  @Column(
      nullable = false,
      length = 30
  )
  private BalanceOperationStatus status;

  @Column(
      name = "balance_after",
      precision = 19,
      scale = 4
  )
  private BigDecimal balanceAfter;

  @Column(
      name = "available_balance_after",
      precision = 19,
      scale = 4
  )
  private BigDecimal availableBalanceAfter;

  @Column(
      nullable = false,
      updatable = false
  )
  private LocalDateTime createdAt;

  private LocalDateTime completedAt;

  @PrePersist
  protected void onCreate() {

    if (createdAt == null) {
      createdAt = LocalDateTime.now();
    }
  }
}