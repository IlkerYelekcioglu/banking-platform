package com.bankcore.customer_service.exception;
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

  @ExceptionHandler(CustomerNotFoundException.class)
  public ResponseEntity<Map<String, Object>> handleCustomerNotFound(
      CustomerNotFoundException exception
  ) {

    Map<String, Object> response = new HashMap<>();

    response.put("timestamp", LocalDateTime.now());
    response.put("status", HttpStatus.NOT_FOUND.value());
    response.put("error", "Customer Not Found");
    response.put("message", exception.getMessage());

    return ResponseEntity
        .status(HttpStatus.NOT_FOUND)
        .body(response);
  }

  @ExceptionHandler(CustomerAlreadyExistsException.class)
  public ResponseEntity<Map<String, Object>> handleCustomerAlreadyExists(
      CustomerAlreadyExistsException exception
  ) {

    Map<String, Object> response = new HashMap<>();

    response.put("timestamp", LocalDateTime.now());
    response.put("status", HttpStatus.CONFLICT.value());
    response.put("error", "Customer Already Exists");
    response.put("message", exception.getMessage());

    return ResponseEntity
        .status(HttpStatus.CONFLICT)
        .body(response);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> handleValidation(
      MethodArgumentNotValidException exception
  ) {

    Map<String, String> errors = new HashMap<>();

    exception.getBindingResult()
        .getFieldErrors()
        .forEach(error ->
            errors.put(
                error.getField(),
                error.getDefaultMessage()
            )
        );

    Map<String, Object> response = new HashMap<>();

    response.put("timestamp", LocalDateTime.now());
    response.put("status", HttpStatus.BAD_REQUEST.value());
    response.put("error", "Validation Error");
    response.put("messages", errors);

    return ResponseEntity
        .badRequest()
        .body(response);
  }
}