package com.banking.customer_service.service;

import com.banking.customer_service.dto.request.CustomerCreateRequest;
import com.banking.customer_service.dto.request.UpdateCustomerRequest;
import com.banking.customer_service.dto.response.CustomerResponse;
import com.banking.customer_service.entity.Customer;
import com.banking.customer_service.exception.CustomerAlreadyExistsException;
import com.banking.customer_service.exception.CustomerNotFoundException;
import com.banking.customer_service.mapper.CustomerMapper;
import com.banking.customer_service.repository.CustomerRepository;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SimpleCustomerService implements CustomerService {

  private final CustomerRepository customerRepository;
  private final CustomerMapper customerMapper;

  @Override
  @Transactional
  public CustomerResponse createCustomer(
      CustomerCreateRequest request
  ) {

    validateUniqueFields(request);

    Customer customer =
        customerMapper.toEntity(request);

    Customer savedCustomer =
        customerRepository.save(customer);

    return customerMapper.toResponse(savedCustomer);
  }

  @Override
  public CustomerResponse getCustomerById(UUID id) {

    Customer customer = customerRepository
        .findById(id)
        .orElseThrow(
            () -> new CustomerNotFoundException(id)
        );

    return customerMapper.toResponse(customer);
  }

  @Override
  public List<CustomerResponse> getAllCustomers() {

    return customerRepository.findAll()
        .stream()
        .map(customerMapper::toResponse)
        .collect(Collectors.toList());
  }

  @Override
  @Transactional
  public CustomerResponse updateCustomer(
      UUID id,
      UpdateCustomerRequest request
  ) {

    Customer customer = customerRepository
        .findById(id)
        .orElseThrow(
            () -> new CustomerNotFoundException(id)
        );

    validateUpdateEmail(customer, request);
    validateUpdateNationalId(customer, request);

    customerMapper.updateEntity(
        customer,
        request
    );

    Customer updatedCustomer =
        customerRepository.save(customer);

    return customerMapper.toResponse(updatedCustomer);
  }

  @Override
  @Transactional
  public void deleteCustomer(UUID id) {

    Customer customer = customerRepository
        .findById(id)
        .orElseThrow(
            () -> new CustomerNotFoundException(id)
        );

    customerRepository.delete(customer);
  }

  private void validateUniqueFields(
      CustomerCreateRequest request
  ) {

    if (customerRepository
        .existsByCustomerNumber(request.getCustomerNumber())) {

      throw new CustomerAlreadyExistsException(
          "Customer number already exists"
      );
    }

    if (customerRepository
        .existsByEmail(request.getEmail())) {

      throw new CustomerAlreadyExistsException(
          "Email already exists"
      );
    }

    if (request.getNationalId() != null &&
        customerRepository
            .existsByNationalId(request.getNationalId())) {

      throw new CustomerAlreadyExistsException(
          "National ID already exists"
      );
    }
  }

  private void validateUpdateEmail(
      Customer customer,
      UpdateCustomerRequest request
  ) {

    if (request.getEmail() != null &&
        !request.getEmail().equals(customer.getEmail()) &&
        customerRepository.existsByEmail(request.getEmail())) {

      throw new CustomerAlreadyExistsException(
          "Email already exists"
      );
    }
  }

  private void validateUpdateNationalId(
      Customer customer,
      UpdateCustomerRequest request
  ) {

    if (request.getNationalId() != null &&
        !request.getNationalId()
            .equals(customer.getNationalId()) &&
        customerRepository
            .existsByNationalId(request.getNationalId())) {

      throw new CustomerAlreadyExistsException(
          "National ID already exists"
      );
    }
  }
}