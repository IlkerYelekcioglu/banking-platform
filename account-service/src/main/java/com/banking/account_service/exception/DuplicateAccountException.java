package com.banking.account_service.exception;

public class DuplicateAccountException extends RuntimeException {

  public DuplicateAccountException(String accountNumber) {
    super("Account number already exists: " + accountNumber);
  }
}
