package com.banking.transaction_service.service;

import com.banking.transaction_service.config.OutboxProperties;
import com.banking.transaction_service.entity.OutboxEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OutboxRetryService {

  private final OutboxProperties outboxProperties;
  private final OutboxEventService outboxEventService;

  public void handleFailure(
      OutboxEvent event,
      Exception exception
  ) {

    int nextRetryCount =
        event.getRetryCount() + 1;

    String failureReason =
        buildFailureReason(exception);

    if (nextRetryCount
        >= outboxProperties.getMaxRetries()) {

      outboxEventService.markAsPermanentlyFailed(
          event.getId(),
          nextRetryCount,
          failureReason
      );

      return;
    }

    long delaySeconds =
        calculateDelaySeconds(
            nextRetryCount
        );

    LocalDateTime nextRetryAt =
        LocalDateTime.now()
            .plusSeconds(delaySeconds);

    outboxEventService.scheduleRetry(
        event.getId(),
        nextRetryCount,
        nextRetryAt,
        failureReason
    );
  }

  private long calculateDelaySeconds(
      int retryCount
  ) {

    long delay =
        (long)
            outboxProperties
                .getInitialDelaySeconds()
            *
            (1L << Math.max(
                0,
                retryCount - 1
            ));

    return Math.min(
        delay,
        outboxProperties
            .getMaxDelaySeconds()
    );
  }

  private String buildFailureReason(
      Exception exception
  ) {

    String message =
        exception.getMessage();

    if (message == null
        || message.isBlank()) {

      return exception
          .getClass()
          .getSimpleName();
    }

    return message.length() > 1000
        ? message.substring(0, 1000)
        : message;
  }
}