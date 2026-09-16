package com.banking.customer_service.dto.response;

import com.banking.customer_service.enums.CustomerStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerResponse {

  private UUID id;

  private String firstName;

  private String lastName;

  private String customerNumber;

  private String email;

  private String phone;

  private String nationalId;

  private LocalDate birthDate;

  private CustomerStatus status;

  private LocalDateTime createdAt;

  private LocalDateTime updatedAt;
}