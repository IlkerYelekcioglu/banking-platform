package com.bankcore.customer_service.entity;

import com.bankcore.customer_service.enums.CustomerStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "customers",
    indexes = {
        @Index(
            name = "idx_customer_number",
            columnList = "customer_number"
        ),
        @Index(
            name = "idx_customer_email",
            columnList = "email"
        ),
        @Index(
            name = "idx_customer_national_id",
            columnList = "national_id"
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer  {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(
      name = "first_name",
      nullable = false,
      length = 100
  )
  private String firstName;

  @Column(
      name = "last_name",
      nullable = false,
      length = 100
  )
  private String lastName;

  @Column(
      name = "customer_number",
      nullable = false,
      unique = true,
      length = 20
  )
  private String customerNumber;

  @Column(
      name = "email",
      nullable = false,
      unique = true,
      length = 150
  )
  private String email;

  @Column(
      name = "phone",
      length = 20
  )
  private String phone;

  @Column(
      name = "national_id",
      unique = true,
      length = 11
  )
  private String nationalId;

  @Column(name = "birth_date")
  private LocalDate birthDate;

  @Enumerated(EnumType.STRING)
  @Column(
      name = "status",
      nullable = false,
      length = 20
  )
  @Builder.Default
  private CustomerStatus status = CustomerStatus.ACTIVE;

  @CreationTimestamp
  @Column(
      name = "created_at",
      nullable = false,
      updatable = false
  )
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;
}