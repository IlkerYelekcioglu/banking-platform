package com.banking.account_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(AccountNotFoundException.class)
  public ResponseEntity<Map<String, Object>> handleAccountNotFound(
      AccountNotFoundException exception) {

    return buildResponse(
        HttpStatus.NOT_FOUND,
        exception.getMessage()
    );
  }

  @ExceptionHandler(DuplicateAccountException.class)
  public ResponseEntity<Map<String, Object>> handleDuplicateAccount(
      DuplicateAccountException exception) {

    return buildResponse(
        HttpStatus.CONFLICT,
        exception.getMessage()
    );
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> handleValidation(
      MethodArgumentNotValidException exception) {

    Map<String, Object> errors = new HashMap<>();

    exception.getBindingResult()
        .getFieldErrors()
        .forEach(error ->
            errors.put(
                error.getField(),
                error.getDefaultMessage()
            )
        );

    return ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(errors);
  }

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<Map<String, Object>> handleIllegalState(
      IllegalStateException exception) {

    return buildResponse(
        HttpStatus.BAD_REQUEST,
        exception.getMessage()
    );
  }

  private ResponseEntity<Map<String, Object>> buildResponse(
      HttpStatus status,
      String message) {

    Map<String, Object> response = new HashMap<>();

    response.put("timestamp", LocalDateTime.now());
    response.put("status", status.value());
    response.put("error", status.getReasonPhrase());
    response.put("message", message);

    return ResponseEntity
        .status(status)
        .body(response);
  }
}
