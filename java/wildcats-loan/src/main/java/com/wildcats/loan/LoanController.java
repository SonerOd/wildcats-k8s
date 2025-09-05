package com.wildcats.loan;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

@RestController
@RequestMapping("/api/loans")
public class LoanController {

    private final RestClient http = RestClient.create();
    private final String txBase;

    public LoanController(@Value("${services.transaction.base-url}") String txBase) {
        this.txBase = txBase;
    }

    @PostMapping("/apply")
    public ResponseEntity<String> apply() {
        try {
            http.post().uri(txBase + "/api/transactions/create").retrieve().toBodilessEntity();
        } catch (Exception e) {
            System.out.println("transaction call failed: " + e.getMessage());
        }
        String body = "{\"loanId\":\"" + UUID.randomUUID() + "\",\"status\":\"processing\"}";
        return ResponseEntity.ok()
                .header("Content-Type","application/json")
                .body(body);
    }
}
