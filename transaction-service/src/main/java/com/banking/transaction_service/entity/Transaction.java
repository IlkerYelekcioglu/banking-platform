package com.banking.transaction_service.entity;

import com.banking.transaction_service.base.BaseEntity;
import com.banking.transaction_service.enums.Currency;
import com.banking.transaction_service.enums.TransactionStatus;
import com.banking.transaction_service.enums.TransactionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
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
    name = "transactions",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_transaction_idempotency_key",
            columnNames = "idempotency_key"
        ),
        @UniqueConstraint(
            name = "uk_transaction_reference",
            columnNames = "transaction_reference"
        )
    },
    indexes = {
        @Index(
            name = "idx_transaction_source_account",
            columnList = "source_account_id"
        ),
        @Index(
            name = "idx_transaction_destination_account",
            columnList = "destination_account_id"
        ),
        @Index(
            name = "idx_transaction_status",
            columnList = "status"
        ),
        @Index(
            name = "idx_compensation_retry",
            columnList = "status,nextCompensationRetryAt"
        ),
        @Index(
            name = "idx_transaction_idempotency",
            columnList = "idempotencyKey"
        )
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction extends BaseEntity {

  @Column(
      nullable = false,
      unique = true,
      length = 50
  )
  private String transactionReference;

  @Column(nullable = false)
  private UUID sourceAccountId;

  @Column(nullable = false)
  private UUID destinationAccountId;

  @Column(
      nullable = false,
      precision = 19,
      scale = 4
  )
  private BigDecimal amount;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Currency currency;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TransactionType transactionType;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TransactionStatus status;

  @Column(length = 255)
  private String description;

  @Column(length = 50)
  private String channel;

  @Column(length = 100)
  private String ipAddress;

  @Column(length = 100)
  private String deviceId;

  @Column(length = 100)
  private String location;

  @Column(nullable = false)
  private LocalDateTime transactionDate;

  @Column(
      precision = 5,
      scale = 2
  )
  private BigDecimal fraudScore;

  @Column(
      nullable = false,
      unique = true,
      length = 100
  )
  private String idempotencyKey;

  @Column(length = 500)
  private String failureReason;

  @Column(nullable = false)
  @Builder.Default
  private boolean debitCompleted = false;

  @Column(nullable = false)
  @Builder.Default
  private boolean creditCompleted = false;

  @Column(nullable = false)
  @Builder.Default
  private boolean compensationCompleted = false;

  @Column(nullable = false)
  @Builder.Default
  private int compensationRetryCount = 0;

  private LocalDateTime nextCompensationRetryAt;

  private LocalDateTime compensationClaimedAt;

  @Column(length = 1000)
  private String compensationFailureReason;

  @Column(
      length = 100,
      unique = true
  )
  private String compensationClaimToken;

  @Version
  private Long version;

}