package com.fooddelivery.orders.repository.mongo;

import com.fooddelivery.orders.document.ReviewCommentDocument;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewCommentRepository extends MongoRepository<ReviewCommentDocument, ObjectId> {
    List<ReviewCommentDocument> findByReviewId(Long reviewId);
}
