package com.fooddelivery.core.repository;

import com.fooddelivery.core.entity.ItemPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemPriceRepository extends JpaRepository<ItemPrice, Long> {
    List<ItemPrice> findByItemIdAndIsActiveTrueOrderByPriorityDesc(Long itemId);
    List<ItemPrice> findByItemIdInAndIsActiveTrueOrderByPriorityDesc(List<Long> itemIds);
}
