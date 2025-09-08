package com.wildcats.tx.kafka;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public class TransactionEvent {

    @JsonProperty("transactionId")
    private String transactionId;

    @JsonProperty("idempotencyKey")
    private String idempotencyKey;

    @JsonProperty("amount")
    private double amount;

    @JsonProperty("status")
    private String status;

    @JsonProperty("eventType")
    private String eventType;

    @JsonProperty("timestamp")
    private LocalDateTime timestamp;

    public TransactionEvent() {}

    public TransactionEvent(String transactionId, String idempotencyKey,
                            double amount, String status, String eventType) {
        this.transactionId = transactionId;
        this.idempotencyKey = idempotencyKey;
        this.amount = amount;
        this.status = status;
        this.eventType = eventType;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}