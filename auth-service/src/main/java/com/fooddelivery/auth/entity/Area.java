package com.fooddelivery.auth.entity;


import com.fooddelivery.auth.enums.AreaType;
import com.fooddelivery.auth.enums.ShipperModel;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;

@Entity
@Table(name = "areas")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Area {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Khóa chính

    @Column(name = "area_code", nullable = false, unique = true, length = 30)
    private String areaCode; // Mã khu vực duy nhất (VD: 'CC_VINHOMES_Q9')

    @Column(name = "area_name", nullable = false, length = 255)
    private String areaName; // Tên khu vực

    @Enumerated(EnumType.STRING)
    @Column(name = "area_type", nullable = false, length = 20)
    private AreaType areaType; // Loại khu vực (CHUNG_CU, KHU_CN, VAN_PHONG, KY_TUC_XA)

    @Column(length = 100)
    private String city; // Thành phố / Tỉnh

    @Column(length = 100)
    private String district; // Quận / Huyện

    @Column(columnDefinition = "TEXT")
    private String address; // Địa chỉ chi tiết

    @Column(name = "center_lat", nullable = false)
    private Double centerLat; // Vĩ độ trung tâm khu vực

    @Column(name = "center_lng", nullable = false)
    private Double centerLng; // Kinh độ trung tâm khu vực

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "boundary_geojson", columnDefinition = "jsonb")
    private Map<String, Object> boundaryGeojson; // Ranh giới polygon định dạng GeoJSON

    @Column(name = "radius_meters")
    private Integer radiusMeters = 500; // Bán kính phục vụ (fallback nếu không có polygon)

    @Column(name = "auth_code", length = 20)
    private String authCode; // Mã bí mật Admin cấp cho cư dân xác thực

    @Enumerated(EnumType.STRING)
    @Column(name = "shipper_model", nullable = false, length = 20)
    private ShipperModel shipperModel = ShipperModel.PLATFORM; // Mô hình vận chuyển

    @Column(name = "is_active")
    private Boolean isActive = true; // Trạng thái hoạt động của khu vực

    @Column(name = "created_by")
    private Long createdBy; // ID của Admin tạo khu vực này

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt; // Thời điểm tạo bản ghi

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt; // Thời điểm cập nhật bản ghi gần nhất
}