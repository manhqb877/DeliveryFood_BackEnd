package com.fooddelivery.payment.repository;

import com.fooddelivery.payment.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByOrderId(Long orderId);
    List<Transaction> findAllByOrderId(Long orderId);
    Optional<Transaction> findByGatewayTransactionId(String gatewayTransactionId);
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);
}
