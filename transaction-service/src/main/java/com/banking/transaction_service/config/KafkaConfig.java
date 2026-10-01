package com.banking.transaction_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.util.backoff.ExponentialBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

  @Bean
  public ProducerFactory<String, String> producerFactory() {

    Map<String, Object> properties =
        new HashMap<>();

    properties.put(
        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
        "localhost:9092"
    );

    properties.put(
        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
        StringSerializer.class
    );

    properties.put(
        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
        StringSerializer.class
    );

    properties.put(
        ProducerConfig.ACKS_CONFIG,
        "all"
    );

    properties.put(
        ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG,
        true
    );

    properties.put(
        ProducerConfig.RETRIES_CONFIG,
        Integer.MAX_VALUE
    );

    properties.put(
        ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION,
        5
    );

    properties.put(
        ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG,
        120000
    );

    properties.put(
        ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG,
        30000
    );

    return new DefaultKafkaProducerFactory<>(
        properties
    );
  }

  @Bean
  public KafkaTemplate<String, String> kafkaTemplate(
      ProducerFactory<String, String> producerFactory
  ) {

    return new KafkaTemplate<>(
        producerFactory
    );
  }

  @Bean
  public ConsumerFactory<String, String> consumerFactory() {

    Map<String, Object> properties =
        new HashMap<>();

    properties.put(
        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
        "localhost:9092"
    );

    properties.put(
        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
        StringDeserializer.class
    );

    properties.put(
        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
        StringDeserializer.class
    );

    properties.put(
        ConsumerConfig.GROUP_ID_CONFIG,
        "transaction-service"
    );

    properties.put(
        ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,
        false
    );

    properties.put(
        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
        "earliest"
    );

    properties.put(
        ConsumerConfig.MAX_POLL_RECORDS_CONFIG,
        100
    );

    properties.put(
        ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG,
        15000
    );

    properties.put(
        ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG,
        300000
    );

    return new DefaultKafkaConsumerFactory<>(
        properties
    );
  }

  @Bean
  public CommonErrorHandler kafkaErrorHandler(
      KafkaTemplate<String, String> kafkaTemplate
  ) {

    DeadLetterPublishingRecoverer recoverer =
        new DeadLetterPublishingRecoverer(
            kafkaTemplate,
            (record, exception) ->
                new TopicPartition(
                    record.topic() + ".DLT",
                    record.partition()
                )
        );

    ExponentialBackOff backOff =
        new ExponentialBackOff(
            2000L,
            2.0
        );

    backOff.setMaxElapsedTime(
        15000L
    );

    return new DefaultErrorHandler(
        recoverer,
        backOff
    );
  }

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, String>
  kafkaListenerContainerFactory(
      ConsumerFactory<String, String> consumerFactory,
      CommonErrorHandler kafkaErrorHandler
  ) {

    ConcurrentKafkaListenerContainerFactory<String, String>
        factory =
        new ConcurrentKafkaListenerContainerFactory<>();

    factory.setConsumerFactory(
        consumerFactory
    );

    factory.setCommonErrorHandler(
        kafkaErrorHandler
    );

    factory.getContainerProperties()
        .setPollTimeout(3000);

    return factory;
  }

  @Bean
  public KafkaAdmin kafkaAdmin() {

    Map<String, Object> configs =
        new HashMap<>();

    configs.put(
        org.apache.kafka.clients.admin.AdminClientConfig
            .BOOTSTRAP_SERVERS_CONFIG,
        "localhost:9092"
    );

    return new KafkaAdmin(configs);
  }


  @Bean
  public NewTopic transactionRequestedTopic() {

    return new NewTopic(
        "bankcore.transaction.requested",
        3,
        (short) 1
    );
  }

  @Bean
  public NewTopic transactionRequestedDltTopic() {

    return new NewTopic(
        "bankcore.transaction.requested.DLT",
        3,
        (short) 1
    );
  }

  @Bean
  public NewTopic fraudDecisionTopic() {

    return new NewTopic(
        "bankcore.fraud.decisions",
        3,
        (short) 1
    );
  }

  @Bean
  public NewTopic fraudDecisionDltTopic() {

    return new NewTopic(
        "bankcore.fraud.decisions.DLT",
        3,
        (short) 1
    );
  }

  @Bean
  public NewTopic transactionEventsTopic() {

    return new NewTopic(
        "bankcore.transaction.events",
        3,
        (short) 1
    );
  }

  @Bean
  public NewTopic transactionEventsDltTopic() {

    return new NewTopic(
        "bankcore.transaction.events.DLT",
        3,
        (short) 1
    );
  }
}