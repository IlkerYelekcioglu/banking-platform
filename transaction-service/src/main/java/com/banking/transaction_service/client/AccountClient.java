package com.banking.transaction_service.client;


import com.banking.transaction_service.dto.request.BalanceOperationRequest;
import com.banking.transaction_service.dto.response.AccountResponse;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(
    name = "account-service",
    url = "${account-service.url}"
)
public interface AccountClient {

  @GetMapping("/api/v1/accounts/{accountId}")
  AccountResponse getAccount(
      @PathVariable("accountId") UUID accountId
  );

  @GetMapping("/api/v1/accounts/{accountId}/balance")
  AccountResponse getBalance(
      @PathVariable("accountId") UUID accountId
  );

  @PostMapping(
      "/api/v1/accounts/{accountId}/debit"
  )
  AccountResponse debit(
      @PathVariable UUID accountId,
      @RequestBody BalanceOperationRequest request,
      @RequestHeader("Idempotency-Key")
      String operationKey
  );

  @PostMapping(
      "/api/v1/accounts/{accountId}/credit"
  )
  AccountResponse credit(
      @PathVariable UUID accountId,
      @RequestBody BalanceOperationRequest request,
      @RequestHeader("Idempotency-Key")
      String operationKey
  );

}