package com.fooddelivery.orders.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CreateReviewRequest {
    @NotNull
    private Long orderId;

    @Min(1) @Max(5)
    private Integer shopRating;
    private String shopComment;

    @Min(1) @Max(5)
    private Integer shipperRating;
    private String shipperComment;

    private List<ProductReviewRequest> productReviews;
    private List<String> imageUrls;
    private Boolean isAnonymous;

    @Data
    public static class ProductReviewRequest {
        @NotNull
        private Long productId;
        @Min(1) @Max(5)
        private Integer rating;
        private String comment;
    }
}
