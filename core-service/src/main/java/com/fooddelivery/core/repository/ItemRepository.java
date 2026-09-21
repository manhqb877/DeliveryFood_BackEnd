package com.fooddelivery.core.repository;

import com.fooddelivery.core.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {
    List<Item> findByShopIdAndStatusNot(Long shopId, com.fooddelivery.core.enums.ItemStatus status);
    List<Item> findByShopIdAndStatusIn(Long shopId, List<com.fooddelivery.core.enums.ItemStatus> statuses);
    List<Item> findByNameContainingIgnoreCaseAndStatusNot(String keyword, com.fooddelivery.core.enums.ItemStatus status);
    List<Item> findByCategoryId(Long categoryId);

    @Modifying
    @Query("UPDATE Item i SET i.dailySold = COALESCE(i.dailySold, 0) + :quantity, " +
           "i.status = CASE WHEN i.dailyLimit IS NOT NULL AND (COALESCE(i.dailySold, 0) + :quantity) >= i.dailyLimit THEN com.fooddelivery.core.enums.ItemStatus.SOLD_OUT ELSE i.status END " +
           "WHERE i.id = :itemId " +
           "AND (i.dailyLimit IS NULL OR (COALESCE(i.dailySold, 0) + :quantity) <= i.dailyLimit) " +
           "AND i.status = com.fooddelivery.core.enums.ItemStatus.AVAILABLE")
    int deductStockAtomic(@Param("itemId") Long itemId, @Param("quantity") int quantity);
}
