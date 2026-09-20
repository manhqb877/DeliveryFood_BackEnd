package com.fooddelivery.orders.service;

import com.fooddelivery.orders.dto.request.ReplyReviewRequest;
import com.fooddelivery.orders.dto.response.ReviewResponse;
import com.fooddelivery.orders.entity.Review;
import com.fooddelivery.orders.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;

    public List<ReviewResponse> getShopReviews(Long shopId) {
        return reviewRepository.findByShopIdOrderByCreatedAtDesc(shopId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ReviewResponse replyReview(Long reviewId, ReplyReviewRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đánh giá với ID: " + reviewId));

        review.setShopReply(request.getReply());
        review.setShopRepliedAt(OffsetDateTime.now());

        Review updated = reviewRepository.save(review);
        log.info("Replied to review ID: {}", reviewId);
        return toResponse(updated);
    }

    private ReviewResponse toResponse(Review r) {
        return ReviewResponse.builder()
                .id(r.getId())
                .orderId(r.getOrderId())
                .userId(r.getUserId())
                .guestSessionId(r.getGuestSessionId())
                .shopId(r.getShopId())
                .shopRating(r.getShopRating())
                .shopComment(r.getShopComment())
                .imageUrls(r.getImageUrls())
                .shopReply(r.getShopReply())
                .shopRepliedAt(r.getShopRepliedAt())
                .isAnonymous(r.getIsAnonymous())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
