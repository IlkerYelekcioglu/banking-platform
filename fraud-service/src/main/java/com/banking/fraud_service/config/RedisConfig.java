package com.banking.fraud_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

@Configuration
public class RedisConfig {

  @Bean
  public RedisTemplate<String, String> redisTemplate(
      org.springframework.data.redis.connection.RedisConnectionFactory connectionFactory) {

    RedisTemplate<String, String> template =
        new RedisTemplate<>();

    template.setConnectionFactory(
        connectionFactory
    );

    template.setEnableTransactionSupport(false);

    return template;
  }

}
