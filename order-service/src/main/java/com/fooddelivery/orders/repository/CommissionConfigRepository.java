package com.fooddelivery.orders.repository;

import com.fooddelivery.orders.entity.CommissionConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommissionConfigRepository extends JpaRepository<CommissionConfig, Long> {
}
