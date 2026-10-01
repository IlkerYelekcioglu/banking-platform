package com.banking.transaction_service.service;

import com.banking.transaction_service.config.OutboxProperties;
import com.banking.transaction_service.entity.OutboxEvent;
import com.banking.transaction_service.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxRecoveryService {

  private final OutboxEventRepository outboxEventRepository;

  private final OutboxProperties outboxProperties;

  @Transactional
  public void recoverStuckEvents() {

    LocalDateTime threshold =
        LocalDateTime.now()
            .minusSeconds(
                outboxProperties
                    .getClaimTimeoutSeconds()
            );

    List<OutboxEvent> stuckEvents =
        outboxEventRepository
            .findTop100ByPublishedFalseAndPermanentlyFailedFalseAndClaimedAtLessThanEqualOrderByClaimedAtAsc(
                threshold
            );

    for (OutboxEvent event : stuckEvents) {

      int releasedRows =
          outboxEventRepository.releaseClaim(
              event.getId()
          );

      if (releasedRows == 1) {

        log.warn(
            "Stuck outbox claim released. " +
                "eventId={}, claimedAt={}",
            event.getId(),
            event.getClaimedAt()
        );
      }
    }
  }
}