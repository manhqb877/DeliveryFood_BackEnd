package com.fooddelivery.auth.entity;


import com.fooddelivery.auth.enums.NodeType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "intra_zone_maps", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"area_id", "node_code"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntraZoneMap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Khóa chính

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "area_id", nullable = false)
    private Area area; // Khu vực chứa nút bản đồ nội bộ này

    @Enumerated(EnumType.STRING)
    @Column(name = "node_type", nullable = false, length = 20)
    private NodeType nodeType; // Loại nút (BUILDING, FLOOR, UNIT, ZONE, WORKSHOP, GATE, LANDMARK)

    @Column(name = "node_code", nullable = false, length = 50)
    private String nodeCode; // Mã nút (VD: 'TOA_A', 'TANG_15')

    @Column(name = "node_label", nullable = false, length = 255)
    private String nodeLabel; // Nhãn hiển thị (VD: "Tòa A", "Cổng B")

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private IntraZoneMap parent; // Quan hệ phân cấp cha-con (Tòa -> Tầng -> Phòng)

    @Column(name = "entry_lat")
    private Double entryLat; // Vĩ độ cửa vào tòa / cổng khu

    @Column(name = "entry_lng")
    private Double entryLng; // Kinh độ cửa vào tòa / cổng khu

    @Column(name = "sort_order")
    private Short sortOrder = 0; // Thứ tự sắp xếp hiển thị

    @Column(name = "is_active")
    private Boolean isActive = true; // Trạng thái hoạt động
}