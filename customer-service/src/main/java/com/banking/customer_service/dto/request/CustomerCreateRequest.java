package com.banking.customer_service.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public class CustomerCreateRequest {

  @NotBlank(message = "First name is required")
  @Size(
      min = 2,
      max = 100,
      message = "First name must be between 2 and 100 characters"
  )
  private String firstName;

  @NotBlank(message = "Last name is required")
  @Size(
      min = 2,
      max = 100,
      message = "Last name must be between 2 and 100 characters"
  )
  private String lastName;

  @NotBlank(message = "Customer number is required")
  @Size(
      max = 20,
      message = "Customer number cannot exceed 20 characters"
  )
  private String customerNumber;

  @NotBlank(message = "Email is required")
  @Email(message = "Invalid email format")
  private String email;

  @Pattern(
      regexp = "^\\+?[0-9]{10,15}$",
      message = "Invalid phone number"
  )
  private String phone;

  @Pattern(
      regexp = "^\\d{11}$",
      message = "National ID must contain 11 digits"
  )
  private String nationalId;

  @Past(message = "Birth date must be in the past")
  private LocalDate birthDate;
}