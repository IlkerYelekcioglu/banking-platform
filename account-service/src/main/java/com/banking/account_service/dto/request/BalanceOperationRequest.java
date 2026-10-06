package com.banking.account_service.dto.request;


import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class BalanceOperationRequest {
  @NotNull
  @DecimalMin(
      value = "0.01",
      message = "Amount must be greater than zero"
  )
  private BigDecimal amount;

  public BalanceOperationRequest(BigDecimal amount) {
    this.amount = amount;
  }

  public BalanceOperationRequest() {
  }
}
