package com.banking.transaction_service.service;

import com.banking.transaction_service.config.CompensationProperties;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CompensationRetryPolicy {

  private final CompensationProperties properties;

  public Duration calculateDelay(int retryCount) {
    if (retryCount < 1) {
      throw new IllegalArgumentException(
          "Retry count must be at least 1."
      );
    }

    long initialDelay =
        Math.max(1, properties.getInitialDelaySeconds());

    long maxDelay =
        Math.max(initialDelay, properties.getMaxDelaySeconds());

    int exponent = Math.min(retryCount - 1, 30);

    long delay;

    try {
      delay = Math.multiplyExact(
          initialDelay,
          1L << exponent
      );
    } catch (ArithmeticException exception) {
      delay = maxDelay;
    }

    return Duration.ofSeconds(
        Math.min(delay, maxDelay)
    );
  }

  public boolean hasRetriesRemaining(int retryCount) {
    return retryCount < properties.getMaxRetries();
  }
}