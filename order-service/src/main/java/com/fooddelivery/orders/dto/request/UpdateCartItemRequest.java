package com.fooddelivery.orders.dto.request;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class UpdateCartItemRequest {
    private Short quantity;
    private List<Map<String, Object>> selectedOptions;
    private String itemNote;
}
