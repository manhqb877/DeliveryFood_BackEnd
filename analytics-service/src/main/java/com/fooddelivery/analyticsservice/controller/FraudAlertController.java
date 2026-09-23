package com.fooddelivery.analyticsservice.controller;

import com.fooddelivery.analyticsservice.document.FraudAlertDocument;
import com.fooddelivery.analyticsservice.dto.FraudAlertDto;
import com.fooddelivery.analyticsservice.enums.FraudAlertStatus;
import com.fooddelivery.analyticsservice.enums.FraudAlertType;
import com.fooddelivery.analyticsservice.enums.FraudSeverity;
import com.fooddelivery.analyticsservice.repository.FraudAlertRepository;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/analytics/admin/fraud-alerts")
@RequiredArgsConstructor
public class FraudAlertController {

    private final FraudAlertRepository fraudAlertRepository;

    @PostConstruct
    public void seedData() {
        if (fraudAlertRepository.count() == 0) {
            FraudAlertDocument alert1 = FraudAlertDocument.builder()
                    .alertType(FraudAlertType.PROMO_ABUSE)
                    .severity(FraudSeverity.HIGH)
                    .userId(1L)
                    .ipAddress("192.168.1.5")
                    .details(Map.of("reason", "Áp mã KM liên tục cùng 1 thiết bị"))
                    .status(FraudAlertStatus.OPEN)
                    .createdAt(Instant.now().minusSeconds(3600))
                    .build();

            FraudAlertDocument alert2 = FraudAlertDocument.builder()
                    .alertType(FraudAlertType.FAKE_ORDER)
                    .severity(FraudSeverity.CRITICAL)
                    .guestSessionId(999L)
                    .ipAddress("113.190.22.11")
                    .details(Map.of("reason", "Tạo nhiều đơn hàng rác không nhận"))
                    .status(FraudAlertStatus.OPEN)
                    .createdAt(Instant.now().minusSeconds(7200))
                    .build();

            fraudAlertRepository.saveAll(List.of(alert1, alert2));
        }
    }

    @GetMapping
    public ResponseEntity<List<FraudAlertDto>> getAllAlerts() {
        List<FraudAlertDto> dtos = fraudAlertRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/internal")
    public ResponseEntity<FraudAlertDto> createInternalAlert(@RequestBody FraudAlertDto payload) {
        FraudAlertDocument doc = FraudAlertDocument.builder()
                .alertType(FraudAlertType.valueOf(payload.getAlertType()))
                .severity(FraudSeverity.valueOf(payload.getSeverity()))
                .userId(payload.getUserId())
                .guestSessionId(payload.getGuestSessionId())
                .ipAddress(payload.getIpAddress())
                .details(Map.of(
                        "reason", payload.getDescription() != null ? payload.getDescription() : "",
                        "orderId", payload.getOrderId() != null ? payload.getOrderId() : "",
                        "shipperId", payload.getShipperId() != null ? payload.getShipperId() : ""
                ))
                .status(FraudAlertStatus.OPEN)
                .createdAt(Instant.now())
                .build();
        FraudAlertDocument saved = fraudAlertRepository.save(doc);
        return ResponseEntity.ok(mapToDto(saved));
    }

    @PostMapping("/{id}/review")
    public ResponseEntity<FraudAlertDto> reviewAlert(@PathVariable String id, @RequestBody Map<String, String> payload) {
        FraudAlertDocument doc = fraudAlertRepository.findById(new ObjectId(id)).orElseThrow();
        doc.setStatus(FraudAlertStatus.valueOf(payload.get("status")));
        doc.setReviewNote(payload.get("note"));
        doc.setReviewedAt(Instant.now());
        // Assume reviewedBy is set via context, hardcoding for now
        doc.setReviewedBy(1L);
        FraudAlertDocument saved = fraudAlertRepository.save(doc);
        return ResponseEntity.ok(mapToDto(saved));
    }

    private FraudAlertDto mapToDto(FraudAlertDocument d) {
        String description = "Cảnh báo hệ thống";
        Long orderId = null;
        Long shipperId = null;
        if (d.getDetails() != null) {
            if (d.getDetails().containsKey("reason")) {
                description = d.getDetails().get("reason").toString();
            }
            if (d.getDetails().containsKey("orderId") && !d.getDetails().get("orderId").toString().isEmpty()) {
                try { orderId = Long.parseLong(d.getDetails().get("orderId").toString()); } catch (Exception e) {}
            }
            if (d.getDetails().containsKey("shipperId") && !d.getDetails().get("shipperId").toString().isEmpty()) {
                try { shipperId = Long.parseLong(d.getDetails().get("shipperId").toString()); } catch (Exception e) {}
            }
        }
        return FraudAlertDto.builder()
                .id(d.getId().toHexString())
                .alertType(d.getAlertType().name())
                .severity(d.getSeverity().name())
                .userId(d.getUserId())
                .guestSessionId(d.getGuestSessionId())
                .ipAddress(d.getIpAddress())
                .deviceFingerprint(d.getDeviceFingerprint())
                .detectedLat(d.getDetectedLat())
                .detectedLng(d.getDetectedLng())
                .description(description)
                .orderId(orderId)
                .shipperId(shipperId)
                .status(d.getStatus().name())
                .reviewedBy(d.getReviewedBy())
                .reviewedAt(d.getReviewedAt())
                .reviewNote(d.getReviewNote())
                .createdAt(d.getCreatedAt())
                .build();
    }
}
