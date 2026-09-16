package com.banking.account_service.dto.request;


import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DepositRequest {

  @NotNull(message = "Amount cannot be null")
  @DecimalMin(
      value = "0.01",
      message = "Amount must be greater than zero"
  )
  private BigDecimal amount;

  @Size(
      max = 250,
      message = "Description cannot exceed 250 characters"
  )
  private String description;

}
