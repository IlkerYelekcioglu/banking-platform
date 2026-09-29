package com.banking.transaction_service.service;

import com.banking.transaction_service.enums.TransactionStatus;
import com.banking.transaction_service.repository.TransactionRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class SimpleCompensationClaimService {

  private final TransactionRepository transactionRepository;

  @Transactional(
      propagation = Propagation.REQUIRES_NEW
  )
  public boolean claim(
      UUID transactionId
  ) {

    int updatedRows =
        transactionRepository.claimCompensation(
            transactionId,
            TransactionStatus.COMPENSATION_REQUIRED,
            TransactionStatus.COMPENSATING,
            LocalDateTime.now()
        );

    return updatedRows == 1;
  }


}
