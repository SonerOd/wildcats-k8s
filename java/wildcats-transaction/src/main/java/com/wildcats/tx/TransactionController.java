package com.wildcats.tx;

import com.wildcats.tx.kafka.TransactionEvent;
import com.wildcats.tx.kafka.TransactionProducer;
import com.wildcats.tx.model.TransactionDoc;
import com.wildcats.tx.repo.TransactionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionRepository repo;
    private final TransactionProducer producer;

    public TransactionController(TransactionRepository repo, TransactionProducer producer) {
        this.repo = repo;
        this.producer = producer;
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
                        TransactionDoc doc = createAndSaveTransaction(amount, idemKey);
                        sendTransactionEvent(doc);
                        return ResponseEntity.ok(doc);
                    });
        }

        TransactionDoc doc = createAndSaveTransaction(amount, null);
        sendTransactionEvent(doc);
        return ResponseEntity.ok(doc);
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
        TransactionEvent event = new TransactionEvent(
                doc.getId(),
                doc.getIdempotencyKey(),
                doc.getAmount(),
                doc.getStatus(),
                "CREATED"
        );
        producer.sendTransactionEvent(event);
    }

    @GetMapping("/list")
    public ResponseEntity<?> list() {
        return ResponseEntity.ok(repo.findAll());
    }

    @PostMapping("/{id}/process")
    public ResponseEntity<?> processTransaction(@PathVariable String id) {
        return repo.findById(id)
                .map(transaction -> {
                    // Manuel olarak transaction process event'i gönder
                    TransactionEvent event = new TransactionEvent(
                            transaction.getId(),
                            transaction.getIdempotencyKey(),
                            transaction.getAmount(),
                            "PROCESSING",
                            "MANUAL_PROCESS"
                    );
                    producer.sendTransactionEvent(event);
                    return ResponseEntity.ok("Transaction processing initiated");
                })
                .orElse(ResponseEntity.notFound().build());
    }
}