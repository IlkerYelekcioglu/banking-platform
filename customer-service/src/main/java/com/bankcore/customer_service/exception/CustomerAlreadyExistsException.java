package com.bankcore.customer_service.exception;

public class CustomerAlreadyExistsException extends RuntimeException {
  public CustomerAlreadyExistsException(String message) {
    super(message);
  }
}
