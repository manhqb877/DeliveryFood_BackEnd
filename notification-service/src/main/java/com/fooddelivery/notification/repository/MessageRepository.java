package com.fooddelivery.notification.repository;

import com.fooddelivery.notification.document.MessageDocument;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends MongoRepository<MessageDocument, ObjectId> {
    List<MessageDocument> findByConversationId(ObjectId conversationId);
}
