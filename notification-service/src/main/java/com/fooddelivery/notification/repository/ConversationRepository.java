package com.fooddelivery.notification.repository;

import com.fooddelivery.notification.document.ConversationDocument;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends MongoRepository<ConversationDocument, ObjectId> {

    List<ConversationDocument> findByOrderId(Long orderId);

    List<ConversationDocument> findByParticipantsShopIdOrderByLastMessageAtDesc(Long shopId);

    List<ConversationDocument> findByParticipantsCustomerIdOrderByLastMessageAtDesc(Long customerId);

    List<ConversationDocument> findByParticipantsGuestSessionIdOrderByLastMessageAtDesc(Long guestSessionId);

    Optional<ConversationDocument> findFirstByParticipantsShopIdAndParticipantsCustomerId(Long shopId, Long customerId);

    Optional<ConversationDocument> findFirstByParticipantsShopIdAndParticipantsCustomerIdAndOrderId(Long shopId, Long customerId, Long orderId);

    Optional<ConversationDocument> findFirstByParticipantsShopIdAndParticipantsGuestSessionId(Long shopId, Long guestSessionId);
}
