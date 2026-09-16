package com.banking.account_service.entity;

import com.banking.account_service.base.BaseEntity;
import com.banking.account_service.enums.AccountStatus;
import com.banking.account_service.enums.AccountType;
import com.banking.account_service.enums.Currency;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
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
@Table(name = "accounts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Account extends BaseEntity {

  @Column(
      nullable = false,
      unique = true,
      length = 26
  )
  private String accountNumber;

  @Column(nullable = false)
  private UUID customerId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AccountType accountType;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Currency currency;

  @Column(
      nullable = false,
      precision = 19,
      scale = 4
  )
  private BigDecimal balance;

  @Column(
      nullable = false,
      precision = 19,
      scale = 4
  )
  private BigDecimal availableBalance;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AccountStatus status;

  @Column(nullable = false)
  private LocalDateTime openedAt;

  private LocalDateTime closedAt;

  @Version
  private Long version;
}