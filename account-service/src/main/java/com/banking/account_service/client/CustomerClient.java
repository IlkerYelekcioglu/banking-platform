package com.banking.account_service.client;

import com.banking.account_service.dto.response.CustomerResponse;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
    name = "customer-service"
)
public interface CustomerClient {

  @GetMapping("/api/v1/customers/{id}")
  CustomerResponse getCustomer(
      @PathVariable("id") UUID id
  );
}