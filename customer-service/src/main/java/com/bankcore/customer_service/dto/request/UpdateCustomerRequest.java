package com.bankcore.customer_service.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCustomerRequest {

  @Size(
      min = 2,
      max = 100,
      message = "First name must be between 2 and 100 characters"
  )
  private String firstName;

  @Size(
      min = 2,
      max = 100,
      message = "Last name must be between 2 and 100 characters"
  )
  private String lastName;

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