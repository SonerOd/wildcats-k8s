package com.wildcats.credit;

import java.util.Random;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

@RestController
@RequestMapping("/api/credit")
public class CreditController {

    private final RestClient http = RestClient.create();
    private final String loanBase;
    private final Random rnd = new Random();
    private final StringRedisTemplate redis;

    public CreditController(
            @Value("${services.loan.base-url}") String loanBase,
            StringRedisTemplate redis) {
        this.loanBase = loanBase;
        this.redis = redis;
    }

    @PostMapping("/check")
    public ResponseEntity<String> check() {
        try {
            http.post().uri(loanBase + "/api/loans/apply").retrieve().toBodilessEntity();
        } catch (Exception e) {
            System.out.println("loan call failed: " + e.getMessage());
        }

        int score = rnd.nextInt(300) + 500;
        boolean approved = score > 650;

        // Redis’ten XYZ oku
        String xyzVal = redis.opsForValue().get("XYZ");
        String xyzField = (xyzVal != null) ? "\"" + xyzVal + "\"" : "\"yoktur : )\"";

        String body = String.format(
                "{\"creditScore\":%d,\"approved\":%s,\"xyz\":%s}",
                score, approved, xyzField
        );

        return ResponseEntity.ok()
                .header("Content-Type","application/json")
                .body(body);
    }
}
