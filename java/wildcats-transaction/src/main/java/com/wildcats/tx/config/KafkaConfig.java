package com.wildcats.tx.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic transactionTopic() {
        return TopicBuilder.name("transaction-events")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic transactionStatusTopic() {
        return TopicBuilder.name("transaction-status")
                .partitions(3)
                .replicas(1)
                .build();
    }
}