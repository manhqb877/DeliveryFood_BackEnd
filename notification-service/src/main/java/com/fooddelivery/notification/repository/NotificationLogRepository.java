package com.fooddelivery.notification.repository;

import com.fooddelivery.notification.document.NotificationLogDocument;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationLogRepository extends MongoRepository<NotificationLogDocument, ObjectId> {
    List<NotificationLogDocument> findByRecipientIdOrderByCreatedAtDesc(Long recipientId);
    List<NotificationLogDocument> findByRecipientIdAndReadAtIsNull(Long recipientId);
    long countByRecipientIdAndReadAtIsNull(Long recipientId);
}
