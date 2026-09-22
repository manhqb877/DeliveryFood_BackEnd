package com.fooddelivery.auth.controller;

import com.fooddelivery.auth.common.ApiResponse;
import com.fooddelivery.auth.entity.ShipperProfile;
import com.fooddelivery.auth.repository.ShipperProfileRepository;
import com.fooddelivery.auth.repository.UserRepository;
import com.fooddelivery.auth.dto.response.ShipperProfileResponse;
import com.fooddelivery.auth.enums.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/auth/shippers")
@RequiredArgsConstructor
public class ShipperProfileController {

    private final ShipperProfileRepository shipperProfileRepository;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ShipperProfileResponse>>> getAllShippers() {
        List<ShipperProfile> shippers = shipperProfileRepository.findAll();
        
        List<ShipperProfileResponse> responseList = shippers.stream().map(s -> ShipperProfileResponse.builder()
                .id(s.getId())
                .user_id(s.getUser().getId())
                .full_name(s.getUser().getFullName())
                .phone(s.getUser().getPhone())
                .id_card_number(s.getIdCardNumber())
                .vehicle_type(s.getVehicleType() != null ? s.getVehicleType().name() : null)
                .vehicle_plate(s.getVehiclePlate())
                .vehicle_photo_url(s.getVehiclePhotoUrl())
                .registered_area_ids(s.getRegisteredAreaIds())
                .approval_status(s.getApprovalStatus() != null ? s.getApprovalStatus().name() : "PENDING")
                .avg_rating(s.getAvgRating())
                .total_deliveries(s.getTotalDeliveries())
                .rejection_reason(s.getRejectionReason())
                .created_at(s.getCreatedAt())
                .build()).toList();
                
        return ResponseEntity.ok(ApiResponse.success("Get all shippers successfully", responseList));
    }

    @org.springframework.web.bind.annotation.PostMapping("/{shipperId}/increment-delivery")
    public ResponseEntity<ApiResponse<Void>> incrementDelivery(@org.springframework.web.bind.annotation.PathVariable Long shipperId) {
        ShipperProfile shipper = shipperProfileRepository.findByUserId(shipperId)
                .orElseThrow(() -> new RuntimeException("Shipper not found"));
        
        shipper.setTotalDeliveries(shipper.getTotalDeliveries() != null ? shipper.getTotalDeliveries() + 1 : 1);
        shipperProfileRepository.save(shipper);
        
        return ResponseEntity.ok(ApiResponse.success("Incremented delivery count successfully", null));
    }

    @org.springframework.web.bind.annotation.PutMapping("/{shipperId}/approve")
    public ResponseEntity<ApiResponse<Void>> approveShipper(
            @org.springframework.web.bind.annotation.PathVariable Long shipperId,
            @org.springframework.web.bind.annotation.RequestParam boolean approved,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String reason) {
        ShipperProfile shipper = shipperProfileRepository.findById(shipperId)
                .orElseThrow(() -> new RuntimeException("Shipper not found"));
        
        shipper.setApprovalStatus(approved ? com.fooddelivery.auth.enums.ApprovalStatus.APPROVED : com.fooddelivery.auth.enums.ApprovalStatus.REJECTED);
        if (!approved && reason != null) {
            shipper.setRejectionReason(reason);
        }
        shipper.setApprovedAt(java.time.OffsetDateTime.now());
        shipperProfileRepository.save(shipper);

        // Khi duyệt shipper: cập nhật trạng thái user thành ACTIVE (vì shipper đăng ký mặc định PENDING)
        // Khi từ chối: giữ nguyên PENDING để shipper có thể chỉnh sửa và đăng ký lại
        if (approved) {
            shipper.getUser().setStatus(UserStatus.ACTIVE);
            userRepository.save(shipper.getUser());
        }
        
        return ResponseEntity.ok(ApiResponse.success("Shipper approval status updated", null));
    }
}
