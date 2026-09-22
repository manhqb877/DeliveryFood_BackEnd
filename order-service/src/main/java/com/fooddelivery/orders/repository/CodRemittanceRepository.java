package com.fooddelivery.orders.repository;

import com.fooddelivery.orders.entity.CodRemittance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CodRemittanceRepository extends JpaRepository<CodRemittance, Long> {
    List<CodRemittance> findByShipperIdOrderByCreatedAtDesc(Long shipperId);
    List<CodRemittance> findByShopIdOrderByCreatedAtDesc(Long shopId);
    Optional<CodRemittance> findByOrderId(Long orderId);
}
