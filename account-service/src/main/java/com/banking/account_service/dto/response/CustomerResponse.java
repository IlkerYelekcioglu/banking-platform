package com.banking.account_service.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;


@Getter
@Setter
public class CustomerResponse {

  private UUID id;

  private String firstName;

  private String lastName;

  private String customerNumber;

  private String email;

  private String phone;

}
