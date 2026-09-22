package com.fooddelivery.orders.service;

import com.fooddelivery.orders.entity.CodRemittance;
import com.fooddelivery.orders.entity.Order;
import com.fooddelivery.orders.enums.PaymentMethod;
import com.fooddelivery.orders.repository.CodRemittanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CodRemittanceService {

    private final CodRemittanceRepository codRemittanceRepository;
    private final RestTemplate restTemplate;

    @Transactional
    public void generateRemittanceIfApplicable(Order order) {
        if (order.getPaymentMethod() != PaymentMethod.COD) return;
        if (codRemittanceRepository.findByOrderId(order.getId()).isPresent()) return; // Already generated

        Long shipperId = null;
        try {
            String trackingUrl = System.getenv().getOrDefault("TRACKING_SERVICE_URL", "http://localhost:8084");
            Map<String, Object> deliveryInfo = restTemplate.getForObject(trackingUrl + "/tracking/deliveries/order/" + order.getId(), Map.class);
            if (deliveryInfo != null && deliveryInfo.get("shipperId") != null) {
                shipperId = Long.valueOf(deliveryInfo.get("shipperId").toString());
            }
        } catch (Exception e) {
            log.warn("Could not fetch delivery info for order {}: {}", order.getId(), e.getMessage());
        }

        if (shipperId == null) {
            log.warn("Cannot generate remittance for order {} because shipperId could not be found", order.getId());
            return;
        }

        CodRemittance remittance = CodRemittance.builder()
                .orderId(order.getId())
                .orderCode(order.getOrderCode())
                .shopId(order.getShopId())
                .shipperId(shipperId)
                .amount(order.getTotalAmount())
                .status("PENDING")
                .build();

        codRemittanceRepository.save(remittance);
        log.info("Generated COD remittance for order {} by shipper {}", order.getId(), shipperId);
    }

    public List<CodRemittance> getShipperRemittances(Long shipperId) {
        return codRemittanceRepository.findByShipperIdOrderByCreatedAtDesc(shipperId);
    }

    public List<CodRemittance> getShopRemittances(Long shopId) {
        return codRemittanceRepository.findByShopIdOrderByCreatedAtDesc(shopId);
    }

    @Transactional
    public CodRemittance completeRemittance(Long remittanceId) {
        CodRemittance remittance = codRemittanceRepository.findById(remittanceId)
                .orElseThrow(() -> new RuntimeException("Remittance not found"));
        remittance.setStatus("COMPLETED");
        remittance.setCompletedAt(OffsetDateTime.now());
        
        try {
            // 1. Fetch shop ownerId from auth-service
            String authUrl = System.getenv().getOrDefault("AUTH_SERVICE_URL", "http://localhost:8081");
            Map<String, Object> shopProfileResponse = restTemplate.getForObject(authUrl + "/auth/shops/" + remittance.getShopId(), Map.class);
            Long ownerId = null;
            if (shopProfileResponse != null && shopProfileResponse.get("data") != null) {
                Map<String, Object> data = (Map<String, Object>) shopProfileResponse.get("data");
                if (data.get("owner_id") != null) ownerId = Long.valueOf(data.get("owner_id").toString());
                else if (data.get("ownerId") != null) ownerId = Long.valueOf(data.get("ownerId").toString());
            }

            // 2. Call payment-service to top-up wallet
            if (ownerId != null) {
                String paymentUrl = System.getenv().getOrDefault("PAYMENT_SERVICE_URL", "http://localhost:8085");
                Map<String, Object> req = Map.of(
                        "ownerId", ownerId,
                        "amount", remittance.getAmount(),
                        "orderId", remittance.getOrderId()
                );
                restTemplate.postForObject(paymentUrl + "/wallets/remit-cod", req, Map.class);
                log.info("Successfully remitted COD to shop owner {} wallet for order {}", ownerId, remittance.getOrderId());
            } else {
                log.warn("Could not find ownerId for shopId {}", remittance.getShopId());
            }
        } catch (Exception e) {
            log.warn("Failed to top-up shop wallet for remittance {}: {}", remittanceId, e.getMessage());
        }

        return codRemittanceRepository.save(remittance);
    }

    public List<CodRemittance> getAllRemittances() {
        return codRemittanceRepository.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
    }
}
