package com.fooddelivery.core.repository;

import com.fooddelivery.core.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    Optional<Promotion> findByCodeAndIsActiveTrue(String code);

    Optional<Promotion> findByCode(String code);

    List<Promotion> findByShopIdOrderByCreatedAtDesc(Long shopId);

    boolean existsByCode(String code);
}
