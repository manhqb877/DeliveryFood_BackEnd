package com.fooddelivery.orders.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewResponse {
    private Long id;
    private Long orderId;
    private Long userId;
    private Long guestSessionId;
    private Long shopId;
    private Short shopRating;
    private String shopComment;
    private String[] imageUrls;
    private String shopReply;
    private OffsetDateTime shopRepliedAt;
    private Boolean isAnonymous;
    private OffsetDateTime createdAt;
}
