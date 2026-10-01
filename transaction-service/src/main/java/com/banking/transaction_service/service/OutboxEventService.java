package com.banking.transaction_service.service;

import com.banking.transaction_service.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxEventService {

  private final OutboxEventRepository outboxEventRepository;

  @Transactional
  public boolean markAsPublished(UUID eventId) {

    int updatedRows =
        outboxEventRepository.markAsPublished(
            eventId,
            LocalDateTime.now()
        );

    return updatedRows == 1;
  }

  @Transactional
  public void releaseClaim(UUID eventId) {

    outboxEventRepository.releaseClaim(eventId);
  }

  @Transactional
  public boolean retryPermanentlyFailed(UUID eventId) {

    int updatedRows =
        outboxEventRepository.resetPermanentlyFailed(
            eventId
        );

    return updatedRows == 1;
  }

  @Transactional
  public void scheduleRetry(
      UUID eventId,
      int retryCount,
      LocalDateTime nextRetryAt,
      String failureReason
  ) {

    outboxEventRepository.scheduleRetry(
        eventId,
        retryCount,
        nextRetryAt,
        failureReason
    );
  }

  @Transactional
  public void markAsPermanentlyFailed(
      UUID eventId,
      int retryCount,
      String failureReason
  ) {

    outboxEventRepository.markAsPermanentlyFailed(
        eventId,
        retryCount,
        failureReason
    );
  }


}
