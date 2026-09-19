package com.fooddelivery.core.repository;

import com.fooddelivery.core.entity.ItemOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemOptionRepository extends JpaRepository<ItemOption, Long> {
    List<ItemOption> findByItemIdInAndIsActiveTrueOrderBySortOrderAsc(List<Long> itemIds);
}
