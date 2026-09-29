package com.banking.transaction_service.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;


@Getter
@Setter
@ConfigurationProperties(
    prefix = "transaction.compensation"
)
public class CompensationProperties {

  private int maxRetries = 5;

  private int initialDelaySeconds = 30;

  private int maxDelaySeconds = 300;

  private int claimTimeoutSeconds = 120;


}
