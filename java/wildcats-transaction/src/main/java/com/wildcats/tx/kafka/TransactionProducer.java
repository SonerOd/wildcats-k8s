package com.wildcats.tx.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import java.util.concurrent.CompletableFuture;

@Service
public class TransactionProducer {

    private static final Logger logger = LoggerFactory.getLogger(TransactionProducer.class);
    private final KafkaTemplate<String, TransactionEvent> kafkaTemplate;

    public TransactionProducer(KafkaTemplate<String, TransactionEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendTransactionEvent(TransactionEvent event) {
        String topic = "transaction-events";

        CompletableFuture<SendResult<String, TransactionEvent>> future =
                kafkaTemplate.send(topic, event.getTransactionId(), event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                logger.info("Sent transaction event: {} with offset: {}",
                        event.getTransactionId(), result.getRecordMetadata().offset());
            } else {
                logger.error("Failed to send transaction event: {}",
                        event.getTransactionId(), ex);
            }
        });
    }
}