package com.banking.transaction_service.service;

import java.util.UUID;

public interface CompensationService {

  void retryCompensation(UUID transactionId,String claimToken);

  void processPendingCompensations();

  void recoverStuckCompensations();

  void manuallyCompensate(UUID transactionId);
}
