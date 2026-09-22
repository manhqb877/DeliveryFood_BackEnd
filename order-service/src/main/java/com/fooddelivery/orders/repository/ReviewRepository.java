package com.fooddelivery.orders.repository;

import com.fooddelivery.orders.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByShopIdOrderByCreatedAtDesc(Long shopId);
    List<Review> findByShipperIdOrderByCreatedAtDesc(Long shipperId);
    boolean existsByOrderId(Long orderId);
    java.util.Optional<Review> findByOrderId(Long orderId);
}
