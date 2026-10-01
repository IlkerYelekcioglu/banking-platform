package com.banking.transaction_service.kafka;

import com.banking.transaction_service.service.DeadLetterEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeadLetterConsumer {

  private final DeadLetterEventService
      deadLetterEventService;


  @KafkaListener(
      topics = {
          KafkaTopics.FRAUD_DECISIONS_DLT,
          KafkaTopics.TRANSACTION_REQUESTED_DLT,
          KafkaTopics.TRANSACTION_EVENTS_DLT
      },
      groupId = "transaction-dlt-service",
      containerFactory = "dltKafkaListenerContainerFactory"
  )
  public void consume(
      ConsumerRecord<String, String> record
  ) {

    log.error(
        "Dead letter event received. " +
            "topic={}, partition={}, offset={}",
        record.topic(),
        record.partition(),
        record.offset()
    );

    String exceptionType =
        getHeader(
            record,
            KafkaHeaders.DLT_EXCEPTION_FQCN
        );

    String exceptionMessage =
        getHeader(
            record,
            KafkaHeaders.DLT_EXCEPTION_MESSAGE
        );

    String consumerGroup =
        getHeader(
            record,
            KafkaHeaders.DLT_ORIGINAL_CONSUMER_GROUP
        );

    deadLetterEventService.saveDeadLetterEvent(
        record.topic(),
        record.partition(),
        record.offset(),
        record.key(),
        record.value(),
        exceptionType,
        exceptionMessage,
        consumerGroup
    );
  }


  private String getHeader(
      ConsumerRecord<String, String> record,
      String headerName
  ) {

    var header =
        record.headers()
            .lastHeader(headerName);

    if (header == null) {
      return null;
    }

    return new String(
        header.value(),
        java.nio.charset.StandardCharsets.UTF_8
    );
  }
}