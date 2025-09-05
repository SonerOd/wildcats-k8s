package com.wildcats.report.api;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class ReportController {
    private final MongoTemplate mongo;

    public ReportController(MongoTemplate mongo) { this.mongo = mongo; }

    @GetMapping("/stats")
    public Map<String,Object> stats() {
        long total = mongo.getCollection("transactions").countDocuments();
        // örnek: toplam amount
        var agg = mongo.getCollection("transactions")
                .aggregate(java.util.List.of(
                        new org.bson.Document("$group",
                                new org.bson.Document("_id", null).append("sum",
                                        new org.bson.Document("$sum", "$amount")))
                )).first();
        double sum = agg != null ? ((Number)agg.get("sum")).doubleValue() : 0.0;
        return Map.of("totalTransactions", total, "totalAmount", sum);
    }
}
