package com.wildcats.tx.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class TransactionProducer {

    private static final Logger logger = LoggerFactory.getLogger(TransactionProducer.class);
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public TransactionProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        logger.info("TransactionProducer initialized with KafkaTemplate");
    }

    public void sendTransactionEvent(TransactionEvent event) {
        try {
            String topic = "transaction-events";
            kafkaTemplate.send(topic, event.getTransactionId(), event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            logger.info("Sent event for transaction: {} to topic: {} with offset: {}",
                                    event.getTransactionId(), topic, result.getRecordMetadata().offset());
                        } else {
                            logger.error("Failed to send event for transaction: {}",
                                    event.getTransactionId(), ex);
                        }
                    });
        } catch (Exception e) {
            logger.error("Error sending transaction event: {}", e.getMessage(), e);
        }
    }
}