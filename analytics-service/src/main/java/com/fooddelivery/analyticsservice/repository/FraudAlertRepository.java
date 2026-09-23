package com.fooddelivery.analyticsservice.repository;

import com.fooddelivery.analyticsservice.document.FraudAlertDocument;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FraudAlertRepository extends MongoRepository<FraudAlertDocument, ObjectId> {
    List<FraudAlertDocument> findAllByOrderByCreatedAtDesc();
}
