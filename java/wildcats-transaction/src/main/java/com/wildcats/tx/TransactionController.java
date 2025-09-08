package com.wildcats.tx;

import com.wildcats.tx.kafka.TransactionEvent;
import com.wildcats.tx.kafka.TransactionProducer;
import com.wildcats.tx.model.TransactionDoc;
import com.wildcats.tx.repo.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private static final Logger logger = LoggerFactory.getLogger(TransactionController.class);

    private final TransactionRepository repo;
    private final TransactionProducer producer;

    @Autowired
    public TransactionController(TransactionRepository repo,
                                 @Autowired(required = false) TransactionProducer producer) {
        this.repo = repo;
        this.producer = producer;
    }

    @PostMapping("/create")
    public ResponseEntity<?> create(
            @RequestHeader(value = "Idempotency-Key", required = false) String idemKey,
            @RequestBody(required = false) Map<String,Object> body) {

        try {
            double amount = (body != null && body.get("amount") instanceof Number)
                    ? ((Number) body.get("amount")).doubleValue()
                    : Math.random() * 10000;

            if (idemKey != null && !idemKey.isBlank()) {
                return repo.findByIdempotencyKey(idemKey)
                        .<ResponseEntity<?>>map(ResponseEntity::ok)
                        .orElseGet(() -> {
                            TransactionDoc doc = createAndSaveTransaction(amount, idemKey);
                            sendTransactionEvent(doc);
                            return ResponseEntity.ok(doc);
                        });
            }

            TransactionDoc doc = createAndSaveTransaction(amount, null);
            sendTransactionEvent(doc);
            return ResponseEntity.ok(doc);

        } catch (Exception e) {
            logger.error("Error creating transaction", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @PostMapping("/{id}/process")
    public ResponseEntity<?> processTransaction(@PathVariable String id) {
        try {
            logger.info("Processing transaction with ID: {}", id);

            return repo.findById(id)
                    .map(transaction -> {
                        logger.info("Found transaction: {}", transaction.getId());

                        // Kafka producer yoksa sadece status güncelle
                        if (producer == null) {
                            logger.warn("Kafka producer is not available, updating status directly");
                            transaction.setStatus("PROCESSING");
                            repo.save(transaction);

                            Map<String, String> response = new HashMap<>();
                            response.put("message", "Transaction status updated (Kafka not available)");
                            response.put("transactionId", transaction.getId());
                            response.put("status", transaction.getStatus());
                            return ResponseEntity.ok(response);
                        }

                        // Kafka producer varsa event gönder
                        TransactionEvent event = new TransactionEvent(
                                transaction.getId(),
                                transaction.getIdempotencyKey(),
                                transaction.getAmount(),
                                "PROCESSING",
                                "MANUAL_PROCESS"
                        );

                        producer.sendTransactionEvent(event);

                        Map<String, String> response = new HashMap<>();
                        response.put("message", "Transaction processing initiated");
                        response.put("transactionId", transaction.getId());
                        return ResponseEntity.ok(response);
                    })
                    .orElseGet(() -> {
                        logger.warn("Transaction not found with ID: {}", id);
                        Map<String, String> error = new HashMap<>();
                        error.put("error", "Transaction not found");
                        error.put("transactionId", id);
                        return ResponseEntity.notFound().build();
                    });

        } catch (Exception e) {
            logger.error("Error processing transaction with ID: " + id, e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("transactionId", id);
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @GetMapping("/list")
    public ResponseEntity<?> list() {
        try {
            return ResponseEntity.ok(repo.findAll());
        } catch (Exception e) {
            logger.error("Error listing transactions", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getTransaction(@PathVariable String id) {
        try {
            return repo.findById(id)
                    .<ResponseEntity<?>>map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            logger.error("Error getting transaction with ID: " + id, e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    private TransactionDoc createAndSaveTransaction(double amount, String idemKey) {
        TransactionDoc doc = new TransactionDoc();
        doc.setAmount(amount);
        doc.setStatus("PENDING");
        if (idemKey != null) {
            doc.setIdempotencyKey(idemKey);
        }
        return repo.save(doc);
    }

    private void sendTransactionEvent(TransactionDoc doc) {
        if (producer != null) {
            try {
                TransactionEvent event = new TransactionEvent(
                        doc.getId(),
                        doc.getIdempotencyKey(),
                        doc.getAmount(),
                        doc.getStatus(),
                        "CREATED"
                );
                producer.sendTransactionEvent(event);
                logger.info("Transaction event sent for ID: {}", doc.getId());
            } catch (Exception e) {
                logger.error("Failed to send transaction event for ID: " + doc.getId(), e);
                // Event gönderilemese bile transaction'ı kaydet
            }
        } else {
            logger.warn("Kafka producer is not available");
        }
    }
}