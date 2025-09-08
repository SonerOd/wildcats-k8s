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
    public TransactionController(TransactionRepository repo, TransactionProducer producer) {
        this.repo = repo;
        this.producer = producer;
        logger.info("TransactionController initialized with producer: {}", producer != null);
    }

    @PostMapping("/create")
    public ResponseEntity<?> create(
            @RequestHeader(value = "Idempotency-Key", required = false) String idemKey,
            @RequestBody(required = false) Map<String,Object> body) {

        double amount = (body != null && body.get("amount") instanceof Number)
                ? ((Number) body.get("amount")).doubleValue()
                : Math.random() * 10000;

        if (idemKey != null && !idemKey.isBlank()) {
            return repo.findByIdempotencyKey(idemKey)
                    .<ResponseEntity<?>>map(ResponseEntity::ok)
                    .orElseGet(() -> {
                        TransactionDoc doc = new TransactionDoc();
                        doc.setAmount(amount);
                        doc.setStatus("PENDING");
                        doc.setIdempotencyKey(idemKey);
                        TransactionDoc saved = repo.save(doc);

                        // Kafka'ya event gönder
                        TransactionEvent event = new TransactionEvent(
                                saved.getId(), saved.getIdempotencyKey(),
                                saved.getAmount(), saved.getStatus(), "CREATED"
                        );
                        producer.sendTransactionEvent(event);

                        return ResponseEntity.ok(saved);
                    });
        }

        TransactionDoc doc = new TransactionDoc();
        doc.setAmount(amount);
        doc.setStatus("PENDING");
        TransactionDoc saved = repo.save(doc);

        // Kafka'ya event gönder
        TransactionEvent event = new TransactionEvent(
                saved.getId(), saved.getIdempotencyKey(),
                saved.getAmount(), saved.getStatus(), "CREATED"
        );
        producer.sendTransactionEvent(event);

        return ResponseEntity.ok(saved);
    }

    @PostMapping("/{id}/process")
    public ResponseEntity<?> processTransaction(@PathVariable String id) {
        logger.info("Processing transaction: {}", id);

        return repo.findById(id)
                .map(transaction -> {
                    // Status güncelle
                    transaction.setStatus("PROCESSING");
                    repo.save(transaction);

                    // Kafka'ya event gönder
                    TransactionEvent event = new TransactionEvent(
                            transaction.getId(),
                            transaction.getIdempotencyKey(),
                            transaction.getAmount(),
                            "PROCESSING",
                            "MANUAL_PROCESS"
                    );
                    producer.sendTransactionEvent(event);

                    Map<String, Object> response = new HashMap<>();
                    response.put("message", "Transaction processing initiated");
                    response.put("transactionId", transaction.getId());
                    response.put("status", transaction.getStatus());

                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/list")
    public ResponseEntity<?> list() {
        return ResponseEntity.ok(repo.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getTransaction(@PathVariable String id) {
        return repo.findById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        Map<String, Object> status = new HashMap<>();
        status.put("service", "UP");
        status.put("kafka", "CONNECTED");
        try {
            status.put("mongodb_count", repo.count());
            status.put("mongodb", "CONNECTED");
        } catch (Exception e) {
            status.put("mongodb", "ERROR: " + e.getMessage());
        }
        return ResponseEntity.ok(status);
    }
}