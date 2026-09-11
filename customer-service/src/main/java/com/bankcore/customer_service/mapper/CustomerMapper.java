package com.bankcore.customer_service.mapper;

import com.bankcore.customer_service.dto.request.CustomerCreateRequest;
import com.bankcore.customer_service.dto.request.UpdateCustomerRequest;
import com.bankcore.customer_service.dto.response.CustomerResponse;
import com.bankcore.customer_service.entity.Customer;
import org.springframework.stereotype.Component;

@Component
public class CustomerMapper {

  public Customer toEntity(CustomerCreateRequest request) {

    return Customer.builder()
        .firstName(request.getFirstName())
        .lastName(request.getLastName())
        .customerNumber(request.getCustomerNumber())
        .email(request.getEmail())
        .phone(request.getPhone())
        .nationalId(request.getNationalId())
        .birthDate(request.getBirthDate())
        .build();
  }

  public CustomerResponse toResponse(Customer customer) {

    return CustomerResponse.builder()
        .id(customer.getId())
        .firstName(customer.getFirstName())
        .lastName(customer.getLastName())
        .customerNumber(customer.getCustomerNumber())
        .email(customer.getEmail())
        .phone(customer.getPhone())
        .nationalId(customer.getNationalId())
        .birthDate(customer.getBirthDate())
        .status(customer.getStatus())
        .createdAt(customer.getCreatedAt())
        .updatedAt(customer.getUpdatedAt())
        .build();
  }

  public void updateEntity(
      Customer customer,
      UpdateCustomerRequest request
  ) {

    if (request.getFirstName() != null) {
      customer.setFirstName(request.getFirstName());
    }

    if (request.getLastName() != null) {
      customer.setLastName(request.getLastName());
    }

    if (request.getEmail() != null) {
      customer.setEmail(request.getEmail());
    }

    if (request.getPhone() != null) {
      customer.setPhone(request.getPhone());
    }

    if (request.getNationalId() != null) {
      customer.setNationalId(request.getNationalId());
    }

    if (request.getBirthDate() != null) {
      customer.setBirthDate(request.getBirthDate());
    }
  }
}