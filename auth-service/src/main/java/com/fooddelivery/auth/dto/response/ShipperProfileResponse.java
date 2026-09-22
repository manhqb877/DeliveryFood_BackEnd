package com.fooddelivery.auth.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
public class ShipperProfileResponse {
    private Long id;
    private Long user_id;
    private String full_name;
    private String phone;
    private String id_card_number;
    private String vehicle_type;
    private String vehicle_plate;
    private String vehicle_photo_url;
    private List<Long> registered_area_ids;
    private String approval_status;
    private BigDecimal avg_rating;
    private Integer total_deliveries;
    private String rejection_reason;
    private OffsetDateTime created_at;
}
