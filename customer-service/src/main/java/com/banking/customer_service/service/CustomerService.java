package com.banking.customer_service.service;

import com.banking.customer_service.dto.request.CustomerCreateRequest;
import com.banking.customer_service.dto.request.UpdateCustomerRequest;
import com.banking.customer_service.dto.response.CustomerResponse;

import java.util.List;
import java.util.UUID;

  public interface CustomerService {

    CustomerResponse createCustomer(
        CustomerCreateRequest request
    );

    CustomerResponse getCustomerById(
        UUID id
    );

    List<CustomerResponse> getAllCustomers();

    CustomerResponse updateCustomer(
        UUID id,
        UpdateCustomerRequest request
    );

    void deleteCustomer(UUID id);
  }
