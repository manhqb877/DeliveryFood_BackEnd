package com.fooddelivery.orders.controller;

import com.fooddelivery.orders.dto.request.ReplyReviewRequest;
import com.fooddelivery.orders.dto.response.ReviewResponse;
import com.fooddelivery.orders.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/shop/{shopId}")
    public ResponseEntity<List<ReviewResponse>> getShopReviews(@PathVariable Long shopId) {
        List<ReviewResponse> responses = reviewService.getShopReviews(shopId);
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{id}/reply")
    public ResponseEntity<ReviewResponse> replyReview(
            @PathVariable Long id,
            @Valid @RequestBody ReplyReviewRequest request
    ) {
        ReviewResponse response = reviewService.replyReview(id, request);
        return ResponseEntity.ok(response);
    }
}
