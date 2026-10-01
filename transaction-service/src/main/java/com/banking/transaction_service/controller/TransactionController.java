package com.banking.transaction_service.controller;

import com.banking.transaction_service.dto.request.TransactionCreateRequest;
import com.banking.transaction_service.dto.response.TransactionResponse;
import com.banking.transaction_service.service.CompensationService;
import com.banking.transaction_service.service.TransactionService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

  private final TransactionService transactionService;

  private final CompensationService compensationService;

  @PostMapping
  public ResponseEntity<TransactionResponse> createTransaction(
      @Valid @RequestBody TransactionCreateRequest request) {

    TransactionResponse response =
        transactionService.createTransaction(request);

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(response);
  }

  @GetMapping("/{transactionId}")
  public ResponseEntity<TransactionResponse> getTransaction(
      @PathVariable UUID transactionId) {

    return ResponseEntity.ok(
        transactionService.getTransaction(transactionId)
    );
  }

  @GetMapping("/reference/{transactionReference}")
  public ResponseEntity<TransactionResponse> getByReference(
      @PathVariable String transactionReference) {

    return ResponseEntity.ok(
        transactionService.getByReference(
            transactionReference
        )
    );
  }

  @GetMapping
  public ResponseEntity<List<TransactionResponse>>
  getAllTransactions() {

    return ResponseEntity.ok(
        transactionService.getAllTransactions()
    );
  }

  @GetMapping("/account/{accountId}")
  public ResponseEntity<List<TransactionResponse>>
  getAccountTransactions(
      @PathVariable UUID accountId) {

    return ResponseEntity.ok(
        transactionService.getAccountTransactions(
            accountId
        )
    );
  }

  @PostMapping("/{transactionId}/compensate")
  public ResponseEntity<Void> compensateTransaction(
      @PathVariable UUID transactionId
  ) {

    compensationService.manuallyCompensate(transactionId);

    return ResponseEntity.noContent().build();
  }



}
