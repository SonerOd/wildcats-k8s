package com.wildcats.tx.repo;

import com.wildcats.tx.model.TransactionDoc;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface TransactionRepository extends MongoRepository<TransactionDoc, String> {
    Optional<TransactionDoc> findByIdempotencyKey(String idempotencyKey);
}
