package com.wildcats.tx.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

@Configuration
@EnableKafka
@ConditionalOnProperty(
        value = "spring.kafka.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class KafkaConfiguration {

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