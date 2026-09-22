package com.fooddelivery.tracking.repository;

import com.fooddelivery.tracking.entity.Delivery;
import com.fooddelivery.tracking.enums.DeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

    Optional<Delivery> findByOrderId(Long orderId);

    List<Delivery> findByShipperIdAndStatusIn(Long shipperId, List<DeliveryStatus> statuses);

    List<Delivery> findByShipperId(Long shipperId);

    @Query("SELECT d FROM Delivery d WHERE d.shipperId IS NULL AND d.status = 'PENDING' ORDER BY d.createdAt DESC")
    List<Delivery> findAvailableDeliveries();

    @Query("SELECT d FROM Delivery d WHERE d.shipperId IS NULL AND d.status = 'PENDING' AND d.areaId = :areaId ORDER BY d.createdAt DESC")
    List<Delivery> findAvailableDeliveriesByArea(@Param("areaId") Long areaId);

    boolean existsByOrderId(Long orderId);

    @Query("SELECT d FROM Delivery d WHERE d.shipperId = :shipperId AND d.status NOT IN ('DELIVERED','FAILED','CANCELLED') ORDER BY d.assignedAt DESC")
    List<Delivery> findActiveDeliveriesByShipper(@Param("shipperId") Long shipperId);

    @Query("SELECT d FROM Delivery d WHERE d.shipperId = :shipperId AND d.status IN ('DELIVERED','FAILED','CANCELLED') ORDER BY d.updatedAt DESC")
    List<Delivery> findHistoryDeliveriesByShipper(@Param("shipperId") Long shipperId);
}
