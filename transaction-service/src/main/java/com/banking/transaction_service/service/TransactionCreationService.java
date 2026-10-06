package com.banking.transaction_service.service;

import com.banking.transaction_service.entity.OutboxEvent;
import com.banking.transaction_service.entity.Transaction;
import com.banking.transaction_service.event.TransactionRequestedEvent;
import com.banking.transaction_service.exception.TransactionProcessingException;
import com.banking.transaction_service.repository.OutboxEventRepository;
import com.banking.transaction_service.repository.TransactionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionCreationService {

  private final TransactionRepository transactionRepository;
  private final OutboxEventRepository outboxEventRepository;
  private final ObjectMapper objectMapper;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Transaction create(
      Transaction transaction
  ) {

    try {

      Transaction savedTransaction =
          transactionRepository.saveAndFlush(
              transaction
          );

      createOutboxEvent(
          savedTransaction
      );

      return savedTransaction;

    } catch (DataIntegrityViolationException exception) {

      throw exception;
    }
  }

  private void createOutboxEvent(
      Transaction transaction
  ) {

    TransactionRequestedEvent event =
        TransactionRequestedEvent.builder()
            .eventId(UUID.randomUUID())
            .transactionId(
                transaction.getId()
            )
            .sourceAccountId(
                transaction.getSourceAccountId()
            )
            .destinationAccountId(
                transaction.getDestinationAccountId()
            )
            .amount(
                transaction.getAmount()
            )
            .currency(
                transaction.getCurrency()
            )
            .transactionType(
                transaction.getTransactionType()
            )
            .channel(
                transaction.getChannel()
            )
            .ipAddress(
                transaction.getIpAddress()
            )
            .deviceId(
                transaction.getDeviceId()
            )
            .location(
                transaction.getLocation()
            )
            .build();

    try {

      String payload =
          objectMapper.writeValueAsString(
              event
          );

      OutboxEvent outboxEvent =
          OutboxEvent.builder()
              .aggregateType("TRANSACTION")
              .aggregateId(
                  transaction.getId()
              )
              .eventType(
                  "TRANSACTION_REQUESTED"
              )
              .payload(payload)
              .published(false)
              .retryCount(0)
              .build();

      outboxEventRepository.save(
          outboxEvent
      );

    } catch (JsonProcessingException exception) {

      throw new TransactionProcessingException(
          "Could not create transaction requested event.",
          exception
      );
    }
  }
}