package com.fooddelivery.core.repository;

import com.fooddelivery.core.entity.PromotionRedemption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PromotionRedemptionRepository extends JpaRepository<PromotionRedemption, Long> {

    long countByPromotionId(Long promotionId);

    long countByPromotionIdAndUserId(Long promotionId, Long userId);

    long countByPromotionIdAndGuestSessionId(Long promotionId, Long guestSessionId);

    List<PromotionRedemption> findByPromotionIdOrderByCreatedAtDesc(Long promotionId);

    boolean existsByPromotionIdAndUserIdAndStatusIn(Long promotionId, Long userId, java.util.Collection<com.fooddelivery.core.enums.RedemptionStatus> statuses);

    java.util.Optional<PromotionRedemption> findFirstByPromotionIdAndUserIdAndStatus(Long promotionId, Long userId, com.fooddelivery.core.enums.RedemptionStatus status);

    long countByPromotionIdAndStatusIn(Long promotionId, java.util.Collection<com.fooddelivery.core.enums.RedemptionStatus> statuses);

    List<PromotionRedemption> findByUserIdAndStatus(Long userId, com.fooddelivery.core.enums.RedemptionStatus status);

    List<PromotionRedemption> findByUserIdAndStatusIn(Long userId, java.util.Collection<com.fooddelivery.core.enums.RedemptionStatus> statuses);
}
