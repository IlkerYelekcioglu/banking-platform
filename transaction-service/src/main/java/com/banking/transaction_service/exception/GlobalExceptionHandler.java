package com.banking.transaction_service.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.banking.transaction_service.dto.ErrorResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(TransactionNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleTransactionNotFound(
      TransactionNotFoundException exception,
      HttpServletRequest request) {

    return buildErrorResponse(
        HttpStatus.NOT_FOUND,
        "TRANSACTION_NOT_FOUND",
        exception.getMessage(),
        request.getRequestURI()
    );
  }

  @ExceptionHandler(InvalidTransactionException.class)
  public ResponseEntity<ErrorResponse> handleInvalidTransaction(
      InvalidTransactionException exception,
      HttpServletRequest request) {

    return buildErrorResponse(
        HttpStatus.BAD_REQUEST,
        "INVALID_TRANSACTION",
        exception.getMessage(),
        request.getRequestURI()
    );
  }

  @ExceptionHandler(InsufficientBalanceException.class)
  public ResponseEntity<ErrorResponse> handleInsufficientBalance(
      InsufficientBalanceException exception,
      HttpServletRequest request) {

    return buildErrorResponse(
        HttpStatus.BAD_REQUEST,
        "INSUFFICIENT_BALANCE",
        exception.getMessage(),
        request.getRequestURI()
    );
  }

  @ExceptionHandler(TransactionProcessingException.class)
  public ResponseEntity<ErrorResponse> handleTransactionProcessing(
      TransactionProcessingException exception,
      HttpServletRequest request) {

    return buildErrorResponse(
        HttpStatus.UNPROCESSABLE_ENTITY,
        "TRANSACTION_PROCESSING_FAILED",
        exception.getMessage(),
        request.getRequestURI()
    );
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgument(
      IllegalArgumentException exception,
      HttpServletRequest request) {

    return buildErrorResponse(
        HttpStatus.BAD_REQUEST,
        "INVALID_REQUEST",
        exception.getMessage(),
        request.getRequestURI()
    );
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGeneralException(
      Exception exception,
      HttpServletRequest request) {

    return buildErrorResponse(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "INTERNAL_SERVER_ERROR",
        "An unexpected error occurred.",
        request.getRequestURI()
    );
  }

  private ResponseEntity<ErrorResponse> buildErrorResponse(
      HttpStatus status,
      String error,
      String message,
      String path) {

    ErrorResponse response = ErrorResponse.builder()
        .timestamp(LocalDateTime.now())
        .status(status.value())
        .error(error)
        .message(message)
        .path(path)
        .build();

    return ResponseEntity
        .status(status)
        .body(response);
  }
  @ExceptionHandler(
      DataIntegrityViolationException.class
  )
  public ResponseEntity<ErrorResponse>
  handleDataIntegrityViolation(
      DataIntegrityViolationException exception
  ) {

    ErrorResponse error =
        new ErrorResponse(
            "DATA_INTEGRITY_VIOLATION",
            "Duplicate or invalid database operation."
        );

    return ResponseEntity
        .status(HttpStatus.CONFLICT)
        .body(error);
  }

  @ExceptionHandler(IdempotencyKeyConflictException.class)
  public ResponseEntity<ErrorResponse>
  handleIdempotencyKeyConflict(
      IdempotencyKeyConflictException exception
  ) {

    ErrorResponse error =
        new ErrorResponse(
            "IDEMPOTENCY_KEY_CONFLICT",
            exception.getMessage()
        );

    return ResponseEntity
        .status(HttpStatus.CONFLICT)
        .body(error);
  }
}