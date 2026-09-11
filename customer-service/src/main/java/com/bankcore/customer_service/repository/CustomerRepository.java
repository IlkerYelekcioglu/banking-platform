package com.bankcore.customer_service.repository;

import com.bankcore.customer_service.entity.Customer;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

  Optional<Customer> findByCustomerNumber(String customerNumber);

  Optional<Customer> findByEmail(String email);

  Optional<Customer> findByNationalId(String nationalId);

  boolean existsByCustomerNumber(String customerNumber);

  boolean existsByEmail(String email);

  boolean existsByNationalId(String nationalId);
}
