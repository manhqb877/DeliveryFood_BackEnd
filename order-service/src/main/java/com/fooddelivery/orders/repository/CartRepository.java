package com.fooddelivery.orders.repository;

import com.fooddelivery.orders.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByUserIdAndShopId(Long userId, Long shopId);
    Optional<Cart> findByGuestSessionIdAndShopId(Long guestSessionId, Long shopId);
    List<Cart> findAllByUserId(Long userId);
    List<Cart> findAllByGuestSessionId(Long guestSessionId);
}
