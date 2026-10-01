package com.banking.account_service.controller;

import com.banking.account_service.dto.request.BalanceOperationRequest;
import com.banking.account_service.dto.response.AccountBalanceResponse;
import com.banking.account_service.dto.request.AccountCreateRequest;
import com.banking.account_service.dto.response.AccountResponse;
import com.banking.account_service.enums.AccountStatus;
import com.banking.account_service.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

  private final AccountService accountService;

  @PostMapping
  public ResponseEntity<AccountResponse> createAccount(
      @Valid @RequestBody AccountCreateRequest request) {

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(accountService.createAccount(request));
  }


  @GetMapping("/{accountId}")
  public ResponseEntity<AccountResponse> getAccount(
      @PathVariable UUID accountId) {

    return ResponseEntity.ok(
        accountService.getAccount(accountId)
    );
  }


  @GetMapping("/customer/{customerId}")
  public ResponseEntity<List<AccountResponse>> getCustomerAccounts(
      @PathVariable UUID customerId) {

    return ResponseEntity.ok(
        accountService.getCustomerAccounts(customerId)
    );
  }

  @GetMapping("/{accountId}/balance")
  public ResponseEntity<AccountBalanceResponse> getBalance(
      @PathVariable UUID accountId) {

    return ResponseEntity.ok(
        accountService.getBalance(accountId)
    );
  }

  @PatchMapping("/{accountId}/status")
  public ResponseEntity<AccountResponse> changeStatus(
      @PathVariable UUID accountId,
      @RequestParam AccountStatus status) {

    return ResponseEntity.ok(
        accountService.changeStatus(
            accountId,
            status
        )
    );
  }

  @PostMapping("/{accountId}/close")
  public ResponseEntity<AccountResponse> closeAccount(
      @PathVariable UUID accountId) {

    return ResponseEntity.ok(
        accountService.closeAccount(accountId)
    );
  }

  @GetMapping
  public ResponseEntity<List<AccountResponse>> getAllAccounts() {

    return ResponseEntity.ok(
        accountService.getAllAccounts()
    );
  }

  @PostMapping("/{accountId}/credit")
  public ResponseEntity<AccountResponse> credit(
      @PathVariable UUID accountId,
 @RequestBody
      BalanceOperationRequest request,

      @RequestHeader("Idempotency-Key")
      String operationKey
  ) {

    AccountResponse response =
        accountService.credit(
            accountId,
            request.getAmount(),
            operationKey
        );

    return ResponseEntity.ok(
        response
    );
  }
}