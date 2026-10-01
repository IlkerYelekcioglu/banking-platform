package com.banking.transaction_service.scheduler;

import com.banking.transaction_service.kafka.OutboxEventPublisher;
import com.banking.transaction_service.service.OutboxRecoveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxScheduler {

  private final OutboxRecoveryService outboxRecoveryService;

  private final OutboxEventPublisher outboxEventPublisher;

  @Scheduled(fixedDelayString = "5000")
  public void processOutbox() {

    try {

      outboxRecoveryService.recoverStuckEvents();

      outboxEventPublisher.publishPendingEvents();

    } catch (Exception exception) {

      log.error(
          "Outbox scheduler failed.",
          exception
      );
    }
  }



}
