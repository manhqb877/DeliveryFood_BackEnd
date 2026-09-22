package com.fooddelivery.tracking.repository;

import com.fooddelivery.tracking.document.LocationSnapshotDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface LocationSnapshotRepository extends MongoRepository<LocationSnapshotDocument, org.bson.types.ObjectId> {

    @Query("{ 'meta.shipper_id': ?0, 'server_received_at': { $gte: ?1 } }")
    List<LocationSnapshotDocument> findByShipperIdAfter(Long shipperId, Instant from);

    @Query("{ 'meta.delivery_id': ?0 }")
    List<LocationSnapshotDocument> findByDeliveryId(Long deliveryId);
}
