package com.fooddelivery.core.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeductStockRequest {

    @NotEmpty(message = "Danh sách món không được để trống")
    private List<DeductItem> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DeductItem {
        private Long itemId;
        private Integer quantity;
    }
}
