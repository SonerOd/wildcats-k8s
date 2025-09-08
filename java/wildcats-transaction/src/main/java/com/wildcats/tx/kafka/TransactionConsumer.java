package com.wildcats.tx.kafka;

import com.wildcats.tx.model.TransactionDoc;
import com.wildcats.tx.repo.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class TransactionConsumer {

    private static final Logger logger = LoggerFactory.getLogger(TransactionConsumer.class);
    private final TransactionRepository repo;
    private final KafkaTemplate<String, TransactionEvent> kafkaTemplate;

    public TransactionConsumer(TransactionRepository repo,
                               KafkaTemplate<String, TransactionEvent> kafkaTemplate) {
        this.repo = repo;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = "transaction-events", groupId = "transaction-service-group")
    public void consumeTransactionEvent(TransactionEvent event) {
        logger.info("Consumed transaction event: {}", event.getTransactionId());

        try {
            // Transaction'ı işle
            processTransaction(event);

            // Status update event'i gönder
            TransactionEvent statusEvent = new TransactionEvent(
                    event.getTransactionId(),
                    event.getIdempotencyKey(),
                    event.getAmount(),
                    "COMPLETED",
                    "STATUS_UPDATE"
            );

            kafkaTemplate.send("transaction-status", statusEvent);

        } catch (Exception e) {
            logger.error("Error processing transaction: {}", event.getTransactionId(), e);

            // Hata durumunda status update
            TransactionEvent errorEvent = new TransactionEvent(
                    event.getTransactionId(),
                    event.getIdempotencyKey(),
                    event.getAmount(),
                    "FAILED",
                    "ERROR"
            );

            kafkaTemplate.send("transaction-status", errorEvent);
        }
    }

    @KafkaListener(topics = "transaction-status", groupId = "status-update-group")
    public void consumeStatusUpdate(TransactionEvent event) {
        logger.info("Status update for transaction: {} - New status: {}",
                event.getTransactionId(), event.getStatus());

        // Database'de status güncelle
        repo.findById(event.getTransactionId()).ifPresent(transaction -> {
            transaction.setStatus(event.getStatus());
            repo.save(transaction);
            logger.info("Updated transaction {} status to {}",
                    event.getTransactionId(), event.getStatus());
        });
    }

    private void processTransaction(TransactionEvent event) {
        // Simüle edilmiş transaction işleme
        logger.info("Processing transaction: {} with amount: {}",
                event.getTransactionId(), event.getAmount());

        // İşlem simülasyonu (örn: ödeme gateway'e gönderme)
        try {
            Thread.sleep(1000); // 1 saniye bekle
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Validation checks
        if (event.getAmount() > 100000) {
            throw new RuntimeException("Amount exceeds limit");
        }
    }
}
