package com.fooddelivery.auth.enums;

/**
 * Loại nút định tuyến nội khu
 */
public enum NodeType {
    BUILDING,  // Tòa nhà (chung cư)
    FLOOR,     // Tầng trong tòa
    UNIT,      // Phòng / Căn hộ cụ thể
    ZONE,      // Khu (KCN: Khu A, Khu B)
    WORKSHOP,  // Xưởng / Nhà máy
    GATE,      // Cổng ra vào
    LANDMARK   // Mốc nội bộ (nhà để xe, siêu thị mini)
}