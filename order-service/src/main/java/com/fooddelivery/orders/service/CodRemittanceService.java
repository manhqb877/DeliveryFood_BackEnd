package com.fooddelivery.orders.service;

import com.fooddelivery.orders.entity.CodRemittance;
import com.fooddelivery.orders.entity.Order;
import com.fooddelivery.orders.entity.OrderItem;
import com.fooddelivery.orders.enums.PaymentMethod;
import com.fooddelivery.orders.repository.CodRemittanceRepository;
import com.fooddelivery.orders.repository.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CodRemittanceService {

    private final CodRemittanceRepository codRemittanceRepository;
    private final OrderItemRepository orderItemRepository;
    private final RestTemplate restTemplate;

    @Transactional
    public void generateRemittanceIfApplicable(Order order) {
        if (order.getPaymentMethod() != PaymentMethod.COD) return;
        if (codRemittanceRepository.findByOrderId(order.getId()).isPresent()) return; // Already generated

        Long shipperId = null;
        Map<String, Object> deliveryInfo = null;
        try {
            String trackingUrl = System.getenv().getOrDefault("TRACKING_SERVICE_URL", "http://localhost:8084");
            deliveryInfo = restTemplate.getForObject(trackingUrl + "/tracking/deliveries/order/" + order.getId(), Map.class);
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

        boolean isMultiShop = false;
        Long shop1Id = order.getShopId();
        Long shop2Id = null;
        String shop1Name = order.getShopName();
        String shop2Name = null;
        BigDecimal shop1Amount = order.getTotalAmount();
        BigDecimal shop2Amount = BigDecimal.ZERO;

        if (deliveryInfo != null && Boolean.TRUE.equals(deliveryInfo.get("isMultiShop"))) {
            isMultiShop = true;
            if (deliveryInfo.get("shop1Id") != null) {
                shop1Id = Long.valueOf(deliveryInfo.get("shop1Id").toString());
            }
            if (deliveryInfo.get("shop1Name") != null) {
                shop1Name = deliveryInfo.get("shop1Name").toString();
            }
            if (deliveryInfo.get("shop2Id") != null) {
                shop2Id = Long.valueOf(deliveryInfo.get("shop2Id").toString());
            }
            if (deliveryInfo.get("shop2Name") != null) {
                shop2Name = deliveryInfo.get("shop2Name").toString();
            }
        } else if (order.getShopName() != null && order.getShopName().contains(" & ")) {
            isMultiShop = true;
            String[] parts = order.getShopName().split(" & ");
            shop1Name = parts[0].trim();
            shop2Name = parts.length > 1 ? parts[1].trim() : "Quán 2";
        }

        if (isMultiShop) {
            try {
                List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
                String coreUrl = System.getenv().getOrDefault("CORE_SERVICE_URL", "http://localhost:8082");
                BigDecimal subtotal1 = BigDecimal.ZERO;
                BigDecimal subtotal2 = BigDecimal.ZERO;

                for (OrderItem item : items) {
                    Long itemShopId = null;
                    try {
                        Map<String, Object> itemData = restTemplate.getForObject(coreUrl + "/core/items/" + item.getItemId(), Map.class);
                        if (itemData != null && itemData.get("shopId") != null) {
                            itemShopId = Long.valueOf(itemData.get("shopId").toString());
                        }
                    } catch (Exception ignored) {}

                    if (itemShopId != null && shop2Id != null && itemShopId.equals(shop2Id)) {
                        subtotal2 = subtotal2.add(item.getTotalPrice() != null ? item.getTotalPrice() : BigDecimal.ZERO);
                    } else if (itemShopId != null && shop1Id != null && itemShopId.equals(shop1Id)) {
                        subtotal1 = subtotal1.add(item.getTotalPrice() != null ? item.getTotalPrice() : BigDecimal.ZERO);
                    } else {
                        if (subtotal1.compareTo(BigDecimal.ZERO) == 0) {
                            subtotal1 = subtotal1.add(item.getTotalPrice() != null ? item.getTotalPrice() : BigDecimal.ZERO);
                        } else {
                            subtotal2 = subtotal2.add(item.getTotalPrice() != null ? item.getTotalPrice() : BigDecimal.ZERO);
                        }
                    }
                }

                BigDecimal totalItems = subtotal1.add(subtotal2);
                if (totalItems.compareTo(BigDecimal.ZERO) > 0) {
                    shop1Amount = order.getTotalAmount()
                            .multiply(subtotal1)
                            .divide(totalItems, 0, RoundingMode.HALF_UP);
                    shop2Amount = order.getTotalAmount().subtract(shop1Amount);
                } else {
                    shop1Amount = order.getTotalAmount().divide(BigDecimal.valueOf(2), 0, RoundingMode.HALF_UP);
                    shop2Amount = order.getTotalAmount().subtract(shop1Amount);
                }
            } catch (Exception ex) {
                log.warn("Error calculating multi-shop breakdown for order {}: {}", order.getId(), ex.getMessage());
                shop1Amount = order.getTotalAmount().divide(BigDecimal.valueOf(2), 0, RoundingMode.HALF_UP);
                shop2Amount = order.getTotalAmount().subtract(shop1Amount);
            }
        }

        CodRemittance remittance = CodRemittance.builder()
                .orderId(order.getId())
                .orderCode(order.getOrderCode())
                .shopId(shop1Id)
                .shipperId(shipperId)
                .amount(order.getTotalAmount())
                .status("PENDING")
                .isMultiShop(isMultiShop)
                .shop2Id(shop2Id)
                .shop1Name(shop1Name)
                .shop2Name(shop2Name)
                .shop1Amount(isMultiShop ? shop1Amount : null)
                .shop2Amount(isMultiShop ? shop2Amount : null)
                .build();

        codRemittanceRepository.save(remittance);
        log.info("Generated COD remittance for order {} by shipper {}: isMultiShop={}, shop1Amount={}, shop2Amount={}",
                order.getId(), shipperId, isMultiShop, shop1Amount, shop2Amount);
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

        if (Boolean.TRUE.equals(remittance.getIsMultiShop()) && remittance.getShop2Id() != null) {
            BigDecimal amt1 = remittance.getShop1Amount() != null ? remittance.getShop1Amount() : remittance.getAmount().divide(BigDecimal.valueOf(2), 0, RoundingMode.HALF_UP);
            BigDecimal amt2 = remittance.getShop2Amount() != null ? remittance.getShop2Amount() : remittance.getAmount().subtract(amt1);

            String name1 = remittance.getShop1Name() != null ? remittance.getShop1Name() : ("Quán #" + remittance.getShopId());
            String name2 = remittance.getShop2Name() != null ? remittance.getShop2Name() : ("Quán #" + remittance.getShop2Id());

            creditShopOwnerWallet(remittance.getShopId(), amt1, remittance.getOrderId(), "Shipper hoàn tiền COD đơn ghép - Quán 1 (" + name1 + ")");
            creditShopOwnerWallet(remittance.getShop2Id(), amt2, remittance.getOrderId(), "Shipper hoàn tiền COD đơn ghép - Quán 2 (" + name2 + ")");
            log.info("Completed multi-shop remittance {} for order {}: Shop 1 ({}) = {}, Shop 2 ({}) = {}",
                    remittanceId, remittance.getOrderId(), remittance.getShopId(), amt1, remittance.getShop2Id(), amt2);
        } else {
            creditShopOwnerWallet(remittance.getShopId(), remittance.getAmount(), remittance.getOrderId(), "Shipper hoàn tiền COD cho đơn #" + remittance.getOrderId());
            log.info("Completed single-shop remittance {} for order {}: Shop {} = {}",
                    remittanceId, remittance.getOrderId(), remittance.getShopId(), remittance.getAmount());
        }

        return codRemittanceRepository.save(remittance);
    }

    private void creditShopOwnerWallet(Long shopId, BigDecimal amount, Long orderId, String description) {
        if (shopId == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return;
        try {
            String authUrl = System.getenv().getOrDefault("AUTH_SERVICE_URL", "http://localhost:8081");
            Map<String, Object> shopProfileResponse = restTemplate.getForObject(authUrl + "/auth/shops/" + shopId, Map.class);
            Long ownerId = null;
            if (shopProfileResponse != null && shopProfileResponse.get("data") != null) {
                Map<String, Object> data = (Map<String, Object>) shopProfileResponse.get("data");
                if (data.get("owner_id") != null) ownerId = Long.valueOf(data.get("owner_id").toString());
                else if (data.get("ownerId") != null) ownerId = Long.valueOf(data.get("ownerId").toString());
            }

            if (ownerId != null) {
                String paymentUrl = System.getenv().getOrDefault("PAYMENT_SERVICE_URL", "http://localhost:8085");
                Map<String, Object> req = Map.of(
                        "ownerId", ownerId,
                        "amount", amount,
                        "orderId", orderId
                );
                restTemplate.postForObject(paymentUrl + "/wallets/remit-cod", req, Map.class);
                log.info("Successfully remitted COD {} to shop owner {} (shopId={}) for order {}", amount, ownerId, shopId, orderId);
            } else {
                log.warn("Could not find ownerId for shopId {}", shopId);
            }
        } catch (Exception e) {
            log.warn("Failed to top-up shop wallet for shopId {}: {}", shopId, e.getMessage());
        }
    }

    public List<CodRemittance> getAllRemittances() {
        return codRemittanceRepository.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
    }
}
