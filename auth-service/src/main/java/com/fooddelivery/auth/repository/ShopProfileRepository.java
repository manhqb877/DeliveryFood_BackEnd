package com.fooddelivery.auth.repository;

import com.fooddelivery.auth.entity.ShopProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShopProfileRepository extends JpaRepository<ShopProfile, Long> {
    List<ShopProfile> findByOwnerId(Long ownerId);
    java.util.Optional<ShopProfile> findFirstByOwnerId(Long ownerId);
    List<ShopProfile> findByApprovalStatus(com.fooddelivery.auth.enums.ApprovalStatus approvalStatus);
}
