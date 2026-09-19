package com.fooddelivery.orders.repository;

import com.fooddelivery.orders.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderCode(String orderCode);
    List<Order> findByUserIdOrderByPlacedAtDesc(Long userId);
    List<Order> findByGuestSessionIdOrderByPlacedAtDesc(Long guestSessionId);
}
