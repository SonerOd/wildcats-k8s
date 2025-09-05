package com.wildcats.tx;

import com.wildcats.tx.model.TransactionDoc;
import com.wildcats.tx.repo.TransactionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionRepository repo;
    public TransactionController(TransactionRepository repo) { this.repo = repo; }

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
                        return ResponseEntity.ok(repo.save(doc));
                    });
        }

        TransactionDoc doc = new TransactionDoc();
        doc.setAmount(amount);
        doc.setStatus("PENDING");
        return ResponseEntity.ok(repo.save(doc));
    }

    @GetMapping("/list")
    public ResponseEntity<?> list() {
        return ResponseEntity.ok(repo.findAll());
    }
}
