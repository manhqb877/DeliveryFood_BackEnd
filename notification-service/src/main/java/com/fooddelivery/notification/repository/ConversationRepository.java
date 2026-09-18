package com.fooddelivery.notification.repository;

import com.fooddelivery.notification.document.ConversationDocument;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversationRepository extends MongoRepository<ConversationDocument, ObjectId> {
    List<ConversationDocument> findByOrderId(Long orderId);
}
