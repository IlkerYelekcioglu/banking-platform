package com.banking.transaction_service.client;


import com.banking.transaction_service.dto.response.AccountResponse;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

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
}