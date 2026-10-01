package com.banking.transaction_service.service;


import com.banking.transaction_service.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxClaimService {

  private final OutboxEventRepository outboxEventRepository;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public boolean claim(UUID eventId) {

    int updatedRows =
        outboxEventRepository.claimEvent(
            eventId,
            LocalDateTime.now()
        );

    return updatedRows == 1;
  }

}
