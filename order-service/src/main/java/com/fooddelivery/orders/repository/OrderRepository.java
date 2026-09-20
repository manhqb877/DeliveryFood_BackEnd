package com.fooddelivery.orders.repository;

import com.fooddelivery.orders.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderCode(String orderCode);
    Optional<Order> findByIdempotencyKey(String idempotencyKey);
    List<Order> findByUserIdOrderByPlacedAtDesc(Long userId);
    List<Order> findByGuestSessionIdOrderByPlacedAtDesc(Long guestSessionId);
    List<Order> findByShopIdOrderByPlacedAtDesc(Long shopId);
    List<Order> findByShopIdAndOrderStatusOrderByPlacedAtDesc(Long shopId, com.fooddelivery.orders.enums.OrderStatus orderStatus);
}
