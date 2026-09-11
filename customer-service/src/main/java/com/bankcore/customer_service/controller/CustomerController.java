package com.bankcore.customer_service.controller;

import com.bankcore.customer_service.dto.request.CustomerCreateRequest;
import com.bankcore.customer_service.dto.request.UpdateCustomerRequest;
import com.bankcore.customer_service.dto.response.CustomerResponse;
import com.bankcore.customer_service.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

  private final CustomerService customerService;

  @PostMapping
  public ResponseEntity<CustomerResponse> createCustomer(
      @Valid @RequestBody CustomerCreateRequest request
  ) {

    CustomerResponse response =
        customerService.createCustomer(request);

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(response);
  }

  @GetMapping("/{id}")
  public ResponseEntity<CustomerResponse> getCustomerById(
      @PathVariable UUID id
  ) {

    CustomerResponse response =
        customerService.getCustomerById(id);

    return ResponseEntity.ok(response);
  }

  @GetMapping
  public ResponseEntity<List<CustomerResponse>> getAllCustomers() {

    List<CustomerResponse> customers =
        customerService.getAllCustomers();

    return ResponseEntity.ok(customers);
  }

  @PutMapping("/{id}")
  public ResponseEntity<CustomerResponse> updateCustomer(
      @PathVariable UUID id,
      @Valid @RequestBody UpdateCustomerRequest request
  ) {

    CustomerResponse response =
        customerService.updateCustomer(id, request);

    return ResponseEntity.ok(response);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteCustomer(
      @PathVariable UUID id
  ) {

    customerService.deleteCustomer(id);

    return ResponseEntity.noContent().build();
  }
}