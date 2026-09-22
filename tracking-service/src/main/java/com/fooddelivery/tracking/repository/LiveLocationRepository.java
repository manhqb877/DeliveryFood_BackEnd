package com.fooddelivery.tracking.repository;

import com.fooddelivery.tracking.entity.LiveLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LiveLocationRepository extends JpaRepository<LiveLocation, Long> {

    Optional<LiveLocation> findByShipperId(Long shipperId);

    @Query("SELECT l FROM LiveLocation l WHERE l.isOnline = true")
    List<LiveLocation> findAllOnlineShippers();

    @Query("SELECT l FROM LiveLocation l WHERE l.deliveryId = :deliveryId")
    Optional<LiveLocation> findByDeliveryId(@Param("deliveryId") Long deliveryId);
}
