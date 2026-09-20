package com.fooddelivery.orders.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReplyReviewRequest {

    @NotBlank(message = "Nội dung phản hồi không được để trống")
    private String reply;
}
