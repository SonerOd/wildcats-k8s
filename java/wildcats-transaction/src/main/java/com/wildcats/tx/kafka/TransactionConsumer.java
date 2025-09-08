package com.wildcats.tx.kafka;

import com.wildcats.tx.model.TransactionDoc;
import com.wildcats.tx.repo.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Random;

@Service
public class TransactionConsumer {

    private static final Logger logger = LoggerFactory.getLogger(TransactionConsumer.class);
    private final TransactionRepository repo;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Random random = new Random();

    @Autowired
    public TransactionConsumer(TransactionRepository repo, KafkaTemplate<String, Object> kafkaTemplate) {
        this.repo = repo;
        this.kafkaTemplate = kafkaTemplate;
        logger.info("TransactionConsumer initialized");
    }

    @KafkaListener(
            topics = "transaction-events",
            groupId = "transaction-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeTransactionEvent(
            @Payload TransactionEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        logger.info("Received event from topic: {}, partition: {}, offset: {}, transactionId: {}, eventType: {}",
                topic, partition, offset, event.getTransactionId(), event.getEventType());

        try {
            // Event type'a göre işle
            switch (event.getEventType()) {
                case "CREATED":
                    handleCreatedEvent(event);
                    break;
                case "MANUAL_PROCESS":
                    handleManualProcessEvent(event);
                    break;
                case "STATUS_UPDATE":
                    handleStatusUpdateEvent(event);
                    break;
                default:
                    logger.warn("Unknown event type: {}", event.getEventType());
            }

        } catch (Exception e) {
            logger.error("Error processing transaction event: {}", event.getTransactionId(), e);
            sendErrorEvent(event, e.getMessage());
        }
    }

    @KafkaListener(
            topics = "transaction-status",
            groupId = "status-update-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeStatusUpdate(@Payload TransactionEvent event) {
        logger.info("Status update received for transaction: {} - New status: {}",
                event.getTransactionId(), event.getStatus());

        try {
            Optional<TransactionDoc> optTransaction = repo.findById(event.getTransactionId());

            if (optTransaction.isPresent()) {
                TransactionDoc transaction = optTransaction.get();
                transaction.setStatus(event.getStatus());
                repo.save(transaction);
                logger.info("Transaction {} status updated to: {}",
                        event.getTransactionId(), event.getStatus());
            } else {
                logger.warn("Transaction not found for status update: {}", event.getTransactionId());
            }

        } catch (Exception e) {
            logger.error("Error updating transaction status: {}", event.getTransactionId(), e);
        }
    }

    private void handleCreatedEvent(TransactionEvent event) {
        logger.info("Processing CREATED event for transaction: {}", event.getTransactionId());

        // Simüle edilmiş işlem - örnek: fraud check
        simulateProcessing("Fraud check", 500);

        // Random olarak bazı transaction'ları reddet (fraud simulation)
        if (event.getAmount() > 50000 || random.nextInt(10) == 0) {
            logger.warn("Transaction {} flagged as suspicious!", event.getTransactionId());
            sendStatusEvent(event, "REJECTED", "FRAUD_CHECK_FAILED");
        } else {
            logger.info("Transaction {} passed fraud check", event.getTransactionId());
            sendStatusEvent(event, "VERIFIED", "FRAUD_CHECK_PASSED");
        }
    }

    private void handleManualProcessEvent(TransactionEvent event) {
        logger.info("Processing MANUAL_PROCESS event for transaction: {}", event.getTransactionId());

        // Uzun süren işlem simülasyonu
        simulateProcessing("Payment processing", 2000);

        // Random success/failure
        if (random.nextInt(10) < 8) { // %80 başarı
            logger.info("Transaction {} processed successfully", event.getTransactionId());
            sendStatusEvent(event, "COMPLETED", "PAYMENT_SUCCESS");

            // Notification event gönder
            sendNotificationEvent(event, "Transaction completed successfully");
        } else {
            logger.error("Transaction {} processing failed", event.getTransactionId());
            sendStatusEvent(event, "FAILED", "PAYMENT_FAILED");

            // Notification event gönder
            sendNotificationEvent(event, "Transaction failed - please retry");
        }
    }

    private void handleStatusUpdateEvent(TransactionEvent event) {
        logger.info("Handling STATUS_UPDATE event for transaction: {}", event.getTransactionId());

        // Database'i güncelle
        repo.findById(event.getTransactionId()).ifPresent(transaction -> {
            String oldStatus = transaction.getStatus();
            transaction.setStatus(event.getStatus());
            repo.save(transaction);

            logger.info("Transaction {} status changed from {} to {}",
                    event.getTransactionId(), oldStatus, event.getStatus());

            // Status değişimi için audit log
            auditLog(event.getTransactionId(), oldStatus, event.getStatus());
        });
    }

    private void sendStatusEvent(TransactionEvent originalEvent, String newStatus, String reason) {
        TransactionEvent statusEvent = new TransactionEvent(
                originalEvent.getTransactionId(),
                originalEvent.getIdempotencyKey(),
                originalEvent.getAmount(),
                newStatus,
                "STATUS_UPDATE"
        );

        kafkaTemplate.send("transaction-status", statusEvent.getTransactionId(), statusEvent)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        logger.info("Status event sent for transaction: {} - Status: {} - Reason: {}",
                                originalEvent.getTransactionId(), newStatus, reason);
                    } else {
                        logger.error("Failed to send status event for transaction: {}",
                                originalEvent.getTransactionId(), ex);
                    }
                });
    }

    private void sendErrorEvent(TransactionEvent originalEvent, String errorMessage) {
        TransactionEvent errorEvent = new TransactionEvent(
                originalEvent.getTransactionId(),
                originalEvent.getIdempotencyKey(),
                originalEvent.getAmount(),
                "ERROR",
                "PROCESSING_ERROR"
        );

        kafkaTemplate.send("transaction-status", errorEvent.getTransactionId(), errorEvent);
        logger.error("Error event sent for transaction: {} - Error: {}",
                originalEvent.getTransactionId(), errorMessage);
    }

    private void sendNotificationEvent(TransactionEvent event, String message) {
        // Notification topic'ine mesaj gönder (opsiyonel)
        logger.info("Notification for transaction {}: {}", event.getTransactionId(), message);
    }

    private void simulateProcessing(String processName, long delayMs) {
        try {
            logger.debug("Simulating {} for {} ms", processName, delayMs);
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Processing interrupted: {}", processName);
        }
    }

    private void auditLog(String transactionId, String oldStatus, String newStatus) {
        logger.info("AUDIT: Transaction {} status changed from {} to {} at {}",
                transactionId, oldStatus, newStatus, System.currentTimeMillis());
    }
}