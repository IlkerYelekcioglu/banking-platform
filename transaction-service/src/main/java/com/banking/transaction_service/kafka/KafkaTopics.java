package com.banking.transaction_service.kafka;

public final class KafkaTopics {

  private KafkaTopics() {
  }

  public static final String TRANSACTION_REQUESTED =
      "bankcore.transaction.requested";

  public static final String TRANSACTION_REQUESTED_DLT =
      "bankcore.transaction.requested.DLT";

  public static final String FRAUD_DECISIONS =
      "bankcore.fraud.decisions";

  public static final String FRAUD_DECISIONS_DLT =
      "bankcore.fraud.decisions.DLT";

  public static final String TRANSACTION_EVENTS =
      "bankcore.transaction.events";

  public static final String TRANSACTION_EVENTS_DLT =
      "bankcore.transaction.events.DLT";
}