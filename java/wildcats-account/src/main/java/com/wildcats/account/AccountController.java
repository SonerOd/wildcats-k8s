package com.wildcats.account;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final RestClient http;
    private final String creditCheckBase;

    public AccountController(@Value("${services.credit-check.base-url}") String creditCheckBase) {
        this.creditCheckBase = creditCheckBase;
        this.http = RestClient.create();
    }

    @PostMapping("/create")
    public ResponseEntity<String> create() {
        try {
            http.post()
                    .uri(creditCheckBase + "/api/credit/check")
                    .retrieve().toBodilessEntity();
        } catch (Exception e) {
            System.out.println("credit-check call failed: " + e.getMessage());
        }
        String body = "{\"id\":\"" + UUID.randomUUID() + "\",\"status\":\"created\"}";
        return ResponseEntity.ok()
                .header("Content-Type","application/json")
                .body(body);
    }
}
