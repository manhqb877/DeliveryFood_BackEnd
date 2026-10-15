package com.fooddelivery.orders.repository;

import com.fooddelivery.orders.entity.CodRemittance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CodRemittanceRepository extends JpaRepository<CodRemittance, Long> {
    List<CodRemittance> findByShipperIdOrderByCreatedAtDesc(Long shipperId);

    @Query("SELECT r FROM CodRemittance r WHERE r.shopId = :shopId OR r.shop2Id = :shopId ORDER BY r.createdAt DESC")
    List<CodRemittance> findByShopIdOrderByCreatedAtDesc(@Param("shopId") Long shopId);

    Optional<CodRemittance> findByOrderId(Long orderId);
}
