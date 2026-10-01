package com.banking.transaction_service.service;

import com.banking.transaction_service.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InboxService {
  private final ProcessedEventRepository processedEventRepository;

  @Transactional
  public boolean markIfNew(
      UUID eventId,
      String eventType,
      UUID aggregateId,
      String consumerName
  ) {

    int insertedRows =
        processedEventRepository.tryMarkAsProcessed(
            eventId,
            eventType,
            aggregateId,
            consumerName,
            LocalDateTime.now()
        );

    return insertedRows == 1;
  }

}
