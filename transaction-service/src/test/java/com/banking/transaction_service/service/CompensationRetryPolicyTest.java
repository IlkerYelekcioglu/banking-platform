package com.banking.transaction_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.banking.transaction_service.config.CompensationProperties;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CompensationRetryPolicyTest {

  private CompensationRetryPolicy policy;

  @BeforeEach
  void setUp() {
    CompensationProperties properties =
        new CompensationProperties();

    properties.setMaxRetries(5);
    properties.setInitialDelaySeconds(30);
    properties.setMaxDelaySeconds(300);

    policy = new CompensationRetryPolicy(properties);
  }

  @Test
  void shouldCalculateExponentialDelay() {
    assertThat(policy.calculateDelay(1))
        .isEqualTo(Duration.ofSeconds(30));

    assertThat(policy.calculateDelay(2))
        .isEqualTo(Duration.ofSeconds(60));

    assertThat(policy.calculateDelay(3))
        .isEqualTo(Duration.ofSeconds(120));

    assertThat(policy.calculateDelay(4))
        .isEqualTo(Duration.ofSeconds(240));

    assertThat(policy.calculateDelay(5))
        .isEqualTo(Duration.ofSeconds(300));
  }

  @Test
  void shouldRejectInvalidRetryCount() {
    assertThatThrownBy(() -> policy.calculateDelay(0))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldReportWhetherRetriesRemain() {
    assertThat(policy.hasRetriesRemaining(0)).isTrue();
    assertThat(policy.hasRetriesRemaining(4)).isTrue();
    assertThat(policy.hasRetriesRemaining(5)).isFalse();
  }
}