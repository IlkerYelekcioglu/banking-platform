package com.banking.fraud_service.service;

import com.banking.fraud_service.repository.ProcessedEventRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SimpleInboxService {

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
