package com.banking.account_service.exception;

import com.banking.account_service.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {


  @ExceptionHandler(AccountNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleAccountNotFound(
      AccountNotFoundException exception,
      HttpServletRequest request) {

    return buildErrorResponse(
        HttpStatus.NOT_FOUND,
        "ACCOUNT_NOT_FOUND",
        exception.getMessage(),
        request.getRequestURI()
    );
  }

  @ExceptionHandler(DuplicateAccountException.class)
  public ResponseEntity<ErrorResponse> handleDuplicateAccount(
      DuplicateAccountException exception,
      HttpServletRequest request) {

    return buildErrorResponse(
        HttpStatus.CONFLICT,
        "DUPLICATE_ACCOUNT",
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

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<ErrorResponse> handleIllegalState(
      IllegalStateException exception,
      HttpServletRequest request) {

    return buildErrorResponse(
        HttpStatus.BAD_REQUEST,
        "INVALID_ACCOUNT_STATE",
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
      ObjectOptimisticLockingFailureException.class
  )
  public ResponseEntity<ErrorResponse> handleOptimisticLocking(
      ObjectOptimisticLockingFailureException exception
  ) {

    ErrorResponse error =
        new ErrorResponse(
            "CONCURRENT_UPDATE",
            "Account was modified by another transaction. Please retry."
        );

    return ResponseEntity
        .status(HttpStatus.CONFLICT)
        .body(error);
  }

  @ExceptionHandler(IdempotencyKeyConflictException.class)
  public ResponseEntity<ErrorResponse> handleIdempotencyKeyConflict(
      IdempotencyKeyConflictException exception) {

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
