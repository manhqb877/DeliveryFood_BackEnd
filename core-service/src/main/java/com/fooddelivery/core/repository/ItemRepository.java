package com.fooddelivery.core.repository;

import com.fooddelivery.core.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {
    List<Item> findByShopIdAndStatusNot(Long shopId, com.fooddelivery.core.enums.ItemStatus status);
    List<Item> findByNameContainingIgnoreCaseAndStatusNot(String keyword, com.fooddelivery.core.enums.ItemStatus status);
}
