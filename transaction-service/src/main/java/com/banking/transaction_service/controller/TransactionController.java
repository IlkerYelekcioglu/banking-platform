package com.banking.transaction_service.controller;

import com.banking.transaction_service.dto.request.TransactionCreateRequest;
import com.banking.transaction_service.dto.response.TransactionResponse;
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

  /**
   * Yeni transaction oluşturur.
   */
  @PostMapping
  public ResponseEntity<TransactionResponse> createTransaction(
      @Valid @RequestBody TransactionCreateRequest request) {

    TransactionResponse response =
        transactionService.createTransaction(request);

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(response);
  }

  /**
   * Transaction ID ile transaction getirir.
   */
  @GetMapping("/{transactionId}")
  public ResponseEntity<TransactionResponse> getTransaction(
      @PathVariable UUID transactionId) {

    return ResponseEntity.ok(
        transactionService.getTransaction(transactionId)
    );
  }

  /**
   * Transaction reference ile transaction getirir.
   */
  @GetMapping("/reference/{transactionReference}")
  public ResponseEntity<TransactionResponse> getByReference(
      @PathVariable String transactionReference) {

    return ResponseEntity.ok(
        transactionService.getByReference(
            transactionReference
        )
    );
  }

  /**
   * Tüm Transaction ları çeker.
   * @return
   */

  @GetMapping
  public ResponseEntity<List<TransactionResponse>>
  getAllTransactions() {

    return ResponseEntity.ok(
        transactionService.getAllTransactions()
    );
  }

  /**
   * Bir hesabın yaptığı ve aldığı transaction'ları getirir.
   */
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
}
