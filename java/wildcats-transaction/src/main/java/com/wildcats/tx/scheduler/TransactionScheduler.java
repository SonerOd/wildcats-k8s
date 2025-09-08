package com.wildcats.tx.scheduler;

import com.wildcats.tx.kafka.TransactionEvent;
import com.wildcats.tx.model.TransactionDoc;
import com.wildcats.tx.repo.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@EnableScheduling
public class TransactionScheduler {

    private static final Logger logger = LoggerFactory.getLogger(TransactionScheduler.class);

    @Autowired
    private TransactionRepository repo;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    // Her 30 saniyede bir pending transaction'ları kontrol et
    @Scheduled(fixedDelay = 30000)
    public void processPendingTransactions() {
        logger.debug("Checking for pending transactions...");

        List<TransactionDoc> pendingTransactions = repo.findByStatus("PENDING");

        if (!pendingTransactions.isEmpty()) {
            logger.info("Found {} pending transactions", pendingTransactions.size());

            for (TransactionDoc transaction : pendingTransactions) {
                // 5 dakikadan eski pending transaction'ları timeout yap
                if (transaction.getCreatedAt() != null &&
                        transaction.getCreatedAt().isBefore(LocalDateTime.now().minusMinutes(5))) {

                    logger.warn("Transaction {} timed out", transaction.getId());

                    TransactionEvent timeoutEvent = new TransactionEvent(
                            transaction.getId(),
                            transaction.getIdempotencyKey(),
                            transaction.getAmount(),
                            "TIMEOUT",
                            "TIMEOUT_CHECK"
                    );

                    kafkaTemplate.send("transaction-status", timeoutEvent);
                }
            }
        }
    }

    // Her 1 dakikada bir metrics log
    @Scheduled(fixedDelay = 60000)
    public void logMetrics() {
        try {
            long total = repo.count();
            long pending = repo.countByStatus("PENDING");
            long completed = repo.countByStatus("COMPLETED");
            long failed = repo.countByStatus("FAILED");

            logger.info("Transaction Metrics - Total: {}, Pending: {}, Completed: {}, Failed: {}",
                    total, pending, completed, failed);
        } catch (Exception e) {
            logger.error("Error logging metrics", e);
        }
    }
}
