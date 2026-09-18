package com.fooddelivery.orders.repository;

import com.fooddelivery.orders.document.CommentLikeDocument;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentLikeRepository extends MongoRepository<CommentLikeDocument, ObjectId> {
    boolean existsByCommentIdAndUserId(ObjectId commentId, Long userId);
}
