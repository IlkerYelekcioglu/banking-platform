package com.banking.account_service.exception;

import java.math.BigDecimal;

public class InsufficientBalanceException extends RuntimeException {

  public InsufficientBalanceException(
      BigDecimal requestedAmount,
      BigDecimal availableBalance) {

    super(
        "Insufficient balance. Requested: "
            + requestedAmount
            + ", Available: "
            + availableBalance
    );
  }
}
