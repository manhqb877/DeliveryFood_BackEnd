package com.fooddelivery.notification.repository;

import com.fooddelivery.notification.document.MessageDocument;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends MongoRepository<MessageDocument, ObjectId> {

    List<MessageDocument> findByConversationId(ObjectId conversationId);

    List<MessageDocument> findByConversationIdOrderByCreatedAtAsc(ObjectId conversationId);

    List<MessageDocument> findByConversationIdAndIsReadFalse(ObjectId conversationId);

    java.util.Optional<MessageDocument> findFirstByConversationIdOrderByCreatedAtDesc(ObjectId conversationId);

    java.util.Optional<MessageDocument> findFirstByConversationIdAndSenderTypeOrderByCreatedAtDesc(ObjectId conversationId, com.fooddelivery.notification.enums.SenderType senderType);
}
