package com.banking.account_service.exception;

public class IdempotencyKeyConflictException extends RuntimeException {

  public IdempotencyKeyConflictException(String message) {
    super(message);
  }
}
