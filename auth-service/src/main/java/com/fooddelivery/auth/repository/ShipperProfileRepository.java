package com.fooddelivery.auth.repository;

import com.fooddelivery.auth.entity.ShipperProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShipperProfileRepository extends JpaRepository<ShipperProfile, Long> {
    Optional<ShipperProfile> findByUserId(Long userId);
}
