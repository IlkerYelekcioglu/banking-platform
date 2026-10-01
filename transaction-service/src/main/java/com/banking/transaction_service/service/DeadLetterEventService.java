package com.banking.transaction_service.service;

import com.banking.transaction_service.dto.response.DeadLetterEventResponse;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DeadLetterEventService {

  void saveDeadLetterEvent(
      String dltTopic,
      int partition,
      long offset,
      String key,
      String payload,
      String exceptionType,
      String exceptionMessage,
      String consumerGroup
  );

  Page<DeadLetterEventResponse> getDeadLetterEvents(
      Pageable pageable
  );

  Page<DeadLetterEventResponse> getFailedDeadLetterEvents(
      Pageable pageable
  );

  DeadLetterEventResponse replay(
      UUID deadLetterEventId
  );


}
