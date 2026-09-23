package com.fooddelivery.orders.service;

import com.fooddelivery.orders.dto.request.CreateReviewRequest;
import com.fooddelivery.orders.dto.request.ReplyReviewRequest;
import com.fooddelivery.orders.dto.response.ReviewResponse;
import com.fooddelivery.orders.entity.Order;
import com.fooddelivery.orders.entity.ProductReview;
import com.fooddelivery.orders.entity.Review;
import com.fooddelivery.orders.repository.OrderRepository;
import com.fooddelivery.orders.repository.ProductReviewRepository;
import com.fooddelivery.orders.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;
    private final ProductReviewRepository productReviewRepository;
    private final RestTemplate restTemplate;

    public List<ReviewResponse> getShopReviews(Long shopId) {
        return reviewRepository.findByShopIdOrderByCreatedAtDesc(shopId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public ReviewResponse getOrderReview(Long orderId) {
        return reviewRepository.findByOrderId(orderId)
                .map(this::toResponse)
                .orElse(null);
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
                .deliveryId(r.getDeliveryId())
                .shipperId(r.getShipperId())
                .shipperRating(r.getShipperRating())
                .shipperComment(r.getShipperComment())
                .imageUrls(r.getImageUrls())
                .shopReply(r.getShopReply())
                .shopRepliedAt(r.getShopRepliedAt())
                .isAnonymous(r.getIsAnonymous())
                .createdAt(r.getCreatedAt())
                .build();
    }
    public List<ReviewResponse> getShipperReviews(Long shipperId) {
        return reviewRepository.findByShipperIdOrderByCreatedAtDesc(shipperId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ReviewResponse createReview(CreateReviewRequest request, Long userId, Long guestSessionId) {
        // Kiểm tra xem đã review chưa
        if (reviewRepository.existsByOrderId(request.getOrderId())) {
            throw new RuntimeException("Đơn hàng này đã được đánh giá!");
        }

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        if (!order.getOrderStatus().name().equals("DELIVERED") && !order.getOrderStatus().name().equals("COMPLETED")) {
            throw new RuntimeException("Chỉ có thể đánh giá khi đơn hàng đã giao thành công!");
        }

        // Validate permission
        if (userId != null && !userId.equals(order.getUserId())) throw new RuntimeException("Unauthorized");
        if (userId == null && guestSessionId != null && !guestSessionId.equals(order.getGuestSessionId())) throw new RuntimeException("Unauthorized");

        Long deliveryId = null;
        Long shipperId = null;
        try {
            String trackingUrl = System.getenv().getOrDefault("TRACKING_SERVICE_URL", "http://localhost:8084");
            Map<String, Object> deliveryInfo = restTemplate.getForObject(trackingUrl + "/tracking/deliveries/order/" + request.getOrderId(), Map.class);
            if (deliveryInfo != null) {
                if (deliveryInfo.get("id") != null) deliveryId = Long.valueOf(deliveryInfo.get("id").toString());
                if (deliveryInfo.get("shipperId") != null) shipperId = Long.valueOf(deliveryInfo.get("shipperId").toString());
            }
        } catch (Exception e) {
            log.warn("Could not fetch delivery info for order {}: {}", request.getOrderId(), e.getMessage());
        }

        Review review = Review.builder()
                .orderId(order.getId())
                .userId(userId)
                .guestSessionId(guestSessionId)
                .shopId(order.getShopId())
                .shopRating(request.getShopRating() != null ? request.getShopRating().shortValue() : null)
                .shopComment(request.getShopComment())
                .deliveryId(deliveryId)
                .shipperId(shipperId)
                .shipperRating(request.getShipperRating() != null ? request.getShipperRating().shortValue() : null)
                .shipperComment(request.getShipperComment())
                .imageUrls(request.getImageUrls() != null ? request.getImageUrls().toArray(new String[0]) : null)
                .isAnonymous(request.getIsAnonymous() != null ? request.getIsAnonymous() : false)
                .build();
        
        Review saved = reviewRepository.save(review);

        if (request.getProductReviews() != null && !request.getProductReviews().isEmpty()) {
            List<ProductReview> productReviews = request.getProductReviews().stream().map(pr -> ProductReview.builder()
                    .orderId(order.getId())
                    .productId(pr.getProductId())
                    .userId(userId != null ? userId : guestSessionId) // Fallback for simplicity
                    .rating(pr.getRating())
                    .comment(pr.getComment())
                    .build()).collect(Collectors.toList());
            productReviewRepository.saveAll(productReviews);
        }

        // --- Fraud Detection Logic ---
        try {
            String analyticsUrl = System.getenv().getOrDefault("ANALYTICS_SERVICE_URL", "http://localhost:8087");
            
            if (request.getShopRating() != null && request.getShopRating() <= 2) {
                Map<String, Object> payload = Map.of(
                        "alertType", "SHOP_FRAUD",
                        "severity", request.getShopRating() == 1 ? "HIGH" : "MEDIUM",
                        "userId", userId != null ? userId : (guestSessionId != null ? guestSessionId : 0),
                        "orderId", order.getId(),
                        "description", "Quán bị đánh giá " + request.getShopRating() + " sao" + (request.getShopComment() != null ? ": " + request.getShopComment() : "")
                );
                restTemplate.postForObject(analyticsUrl + "/analytics/admin/fraud-alerts/internal", payload, Map.class);
            }

            if (request.getShipperRating() != null && request.getShipperRating() <= 2) {
                Map<String, Object> payload = Map.of(
                        "alertType", "SHIPPER_FRAUD",
                        "severity", request.getShipperRating() == 1 ? "HIGH" : "MEDIUM",
                        "userId", userId != null ? userId : (guestSessionId != null ? guestSessionId : 0),
                        "orderId", order.getId(),
                        "shipperId", shipperId != null ? shipperId : 0,
                        "description", "Shipper bị đánh giá " + request.getShipperRating() + " sao" + (request.getShipperComment() != null ? ": " + request.getShipperComment() : "")
                );
                restTemplate.postForObject(analyticsUrl + "/analytics/admin/fraud-alerts/internal", payload, Map.class);
            }
        } catch (Exception e) {
            log.warn("Could not create fraud alert: {}", e.getMessage());
        }

        return toResponse(saved);
    }

    public List<com.fooddelivery.orders.entity.Review> getAllReviews() {
        return reviewRepository.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
    }
}
