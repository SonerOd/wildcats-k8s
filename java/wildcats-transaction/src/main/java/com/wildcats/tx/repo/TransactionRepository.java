package com.wildcats.tx.repo;

import com.wildcats.tx.model.TransactionDoc;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends MongoRepository<TransactionDoc, String> {
    Optional<TransactionDoc> findByIdempotencyKey(String idempotencyKey);
    List<TransactionDoc> findByStatus(String status);
    long countByStatus(String status);
}
