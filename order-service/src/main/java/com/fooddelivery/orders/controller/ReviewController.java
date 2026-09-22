package com.fooddelivery.orders.controller;

import com.fooddelivery.orders.dto.request.ReplyReviewRequest;
import com.fooddelivery.orders.dto.response.ReviewResponse;
import com.fooddelivery.orders.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final com.fooddelivery.orders.repository.ProductReviewRepository productReviewRepository;

    @GetMapping("/shop/{shopId}")
    public ResponseEntity<List<ReviewResponse>> getShopReviews(@PathVariable Long shopId) {
        List<ReviewResponse> responses = reviewService.getShopReviews(shopId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<ReviewResponse> getOrderReview(@PathVariable Long orderId) {
        ReviewResponse response = reviewService.getOrderReview(orderId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/reply")
    public ResponseEntity<ReviewResponse> replyReview(
            @PathVariable Long id,
            @Valid @RequestBody ReplyReviewRequest request
    ) {
        ReviewResponse response = reviewService.replyReview(id, request);
        return ResponseEntity.ok(response);
    }
    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(
            @Valid @RequestBody com.fooddelivery.orders.dto.request.CreateReviewRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Guest-Session-Id", required = false) Long guestSessionId
    ) {
        ReviewResponse response = reviewService.createReview(request, userId, guestSessionId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/shipper/{shipperId}")
    public ResponseEntity<List<ReviewResponse>> getShipperReviews(@PathVariable Long shipperId) {
        List<ReviewResponse> responses = reviewService.getShipperReviews(shipperId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<com.fooddelivery.orders.entity.ProductReview>> getProductReviews(@PathVariable Long productId) {
        List<com.fooddelivery.orders.entity.ProductReview> reviews = 
            productReviewRepository.findByProductIdOrderByCreatedAtDesc(productId);
        return ResponseEntity.ok(reviews);
    }

    @GetMapping("/product-reviews/order/{orderId}")
    public ResponseEntity<List<com.fooddelivery.orders.entity.ProductReview>> getProductReviewsByOrderId(@PathVariable Long orderId) {
        List<com.fooddelivery.orders.entity.ProductReview> reviews = 
            productReviewRepository.findByOrderId(orderId);
        return ResponseEntity.ok(reviews);
    }

    // Admin endpoint: all reviews (for khieu nai / complaints tab)
    @GetMapping("/admin/all")
    public ResponseEntity<List<ReviewResponse>> getAllReviewsAdmin() {
        List<com.fooddelivery.orders.entity.Review> all = reviewService.getAllReviews();
        List<ReviewResponse> responses = all.stream().map(r -> ReviewResponse.builder()
                .id(r.getId())
                .orderId(r.getOrderId())
                .userId(r.getUserId())
                .guestSessionId(r.getGuestSessionId())
                .shopId(r.getShopId())
                .shopRating(r.getShopRating())
                .shopComment(r.getShopComment())
                .deliveryId(r.getDeliveryId())
                .shipperId(r.getShipperId())
                .shipperRating(r.getShipperRating())
                .shipperComment(r.getShipperComment())
                .imageUrls(r.getImageUrls())
                .shopReply(r.getShopReply())
                .shopRepliedAt(r.getShopRepliedAt())
                .isAnonymous(r.getIsAnonymous())
                .createdAt(r.getCreatedAt())
                .build()).collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(responses);
    }
}
