package com.banking.transaction_service.scheduler;

import com.banking.transaction_service.service.CompensationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
@Slf4j
public class CompensationScheduler {

  private final CompensationService compensationService;

  @Scheduled(
      fixedDelayString = "30000"
  )
  public void retryPendingCompensations() {

    log.debug(
        "Starting compensation scheduler."
    );

    compensationService
        .processPendingCompensations();
  }
}