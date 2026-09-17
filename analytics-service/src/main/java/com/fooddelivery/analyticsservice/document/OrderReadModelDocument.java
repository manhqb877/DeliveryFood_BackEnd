package com.fooddelivery.analyticsservice.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Document(collection = "order_read_models")
@CompoundIndexes({
        @CompoundIndex(name = "idx_area_placed", def = "{ 'areaId': 1, 'placedAt': -1 }"),
        @CompoundIndex(name = "idx_shop_placed", def = "{ 'shopId': 1, 'placedAt': -1 }"),
        @CompoundIndex(name = "idx_status_placed", def = "{ 'finalStatus': 1, 'placedAt': -1 }")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderReadModelDocument {

    @Id
    private Long id; // Khớp với order_db.orders.id (Long)

    @Field("order_code")
    private String orderCode; // Mã đơn hàng

    @Field("area_id")
    @Indexed
    private Long areaId; // ID khu vực

    @Field("shop_id")
    @Indexed
    private Long shopId; // ID gian hàng

    @Field("user_id")
    @Indexed
    private Long userId; // ID Khách hàng

    @Field("total_amount")
    private BigDecimal totalAmount; // Tổng tiền thanh toán

    @Field("discount_amount")
    private BigDecimal discountAmount; // Số tiền được giảm

    @Field("payment_method")
    private String paymentMethod; // Phương thức thanh toán (ONLINE, COD, WALLET)

    @Field("final_status")
    private String finalStatus; // Trạng thái cuối cùng của đơn (COMPLETED, CANCELLED)

    // Tọa độ phục vụ phân tích địa lý / Heat map
    @Field("shop_lat")
    private Double shopLat;

    @Field("shop_lng")
    private Double shopLng;

    @Field("delivery_lat")
    private Double deliveryLat;

    @Field("delivery_lng")
    private Double deliveryLng;

    @Field("shipper_id")
    private Long shipperId; // ID Shipper giao hàng

    @Field("delivery_duration_s")
    private Integer deliveryDurationS; // Thời gian giao thực tế (giây)

    @Field("prep_duration_s")
    private Integer prepDurationS; // Thời gian quán chuẩn bị (giây)

    @Field("placed_at")
    private Instant placedAt; // Thời điểm đặt đơn

    @Field("completed_at")
    private Instant completedAt; // Thời điểm hoàn tất đơn

    @Field("items")
    private List<OrderItemSnapshot> items; // Danh sách món ăn trong đơn (denormalized)

    // Metadata phục vụ Machine Learning / Analytics
    @Field("hour_of_day")
    private Integer hourOfDay; // Giờ trong ngày (0-23)

    @Field("day_of_week")
    private Integer dayOfWeek; // Thứ trong tuần (0=CN, 1=T2... 6=T7)

    @Field("is_weekend")
    private Boolean isWeekend; // Có phải cuối tuần không

    @Field("updated_at")
    private Instant updatedAt = Instant.now();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderItemSnapshot {
        @Field("item_id")
        private Long itemId;

        @Field("name")
        private String name;

        @Field("quantity")
        private Integer quantity;

        @Field("price")
        private BigDecimal price;
    }
}