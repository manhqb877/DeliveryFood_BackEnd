package com.fooddelivery.tracking.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fooddelivery.tracking.dto.event.OrderEvent;
import com.fooddelivery.tracking.entity.Delivery;
import com.fooddelivery.tracking.enums.ConfirmMethod;
import com.fooddelivery.tracking.enums.DeliveryStatus;
import com.fooddelivery.tracking.repository.DeliveryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private final DeliveryRepository deliveryRepository;
    private final ObjectMapper objectMapper;

    private static final String VIETMAP_API_KEY = "809bdd000025b62b0e9710b82e28f65f6178ee698cdb1845";
    // URL core-service để lấy thông tin shop (toạ độ)
    private static final String CORE_SERVICE_URL = "http://core-service:8082";

    @KafkaListener(topics = "order-events", groupId = "tracking-service-group")
    @Transactional
    public void handleOrderEvent(String message) {
        try {
            OrderEvent event = objectMapper.readValue(message, OrderEvent.class);
            log.info("Received OrderEvent from Kafka: {}", event.getEventType());

            if ("ORDER_STATUS_CHANGED".equals(event.getEventType())) {
                String status = event.getOrderStatus();
                log.info("Order {} status changed to {}", event.getOrderId(), status);

                if ("CONFIRMED".equals(status) || "READY_FOR_PICKUP".equals(status) || "PREPARING".equals(status)) {
                    createDeliveryIfNotExists(event);
                }
            }
        } catch (Exception e) {
            log.error("Error processing Kafka message: {}", e.getMessage(), e);
        }
    }

    private void createDeliveryIfNotExists(OrderEvent event) {
        Optional<Delivery> existing = deliveryRepository.findByOrderId(event.getOrderId());
        if (existing.isPresent()) {
            log.info("Delivery already exists for order {}, updating if needed", event.getOrderId());
            // Cập nhật địa chỉ nếu trước đó bị "Địa chỉ mặc định"
            Delivery d = existing.get();
            if ("Địa chỉ mặc định".equals(d.getDeliveryAddress()) && event.getDeliveryAddress() != null) {
                d.setDeliveryAddress(event.getDeliveryAddress());
                // Geocode lại nếu toạ độ là mặc định
                if (d.getDeliveryLat() != null && Math.abs(d.getDeliveryLat() - 10.8430) < 0.001) {
                    double[] coords = geocodeAddress(event.getDeliveryAddress());
                    if (coords != null) {
                        d.setDeliveryLat(coords[0]);
                        d.setDeliveryLng(coords[1]);
                        log.info("Updated delivery coords for order {}: {}, {}", event.getOrderId(), coords[0], coords[1]);
                    }
                }
                deliveryRepository.save(d);
            }
            return;
        }

        // === Lấy toạ độ GIAO HÀNG (địa chỉ khách) ===
        String deliveryAddress = event.getDeliveryAddress() != null ? event.getDeliveryAddress() : "Địa chỉ mặc định";
        double deliveryLat = event.getDeliveryLat() != null ? event.getDeliveryLat() : 10.8430;
        double deliveryLng = event.getDeliveryLng() != null ? event.getDeliveryLng() : 106.8450;

        // Nếu event không chứa toạ độ thật (bị rơi về default) thì mới geocode
        if ((event.getDeliveryLat() == null || event.getDeliveryLat() == 0) && event.getDeliveryAddress() != null && !event.getDeliveryAddress().isBlank()) {
            double[] coords = geocodeAddress(event.getDeliveryAddress());
            if (coords != null) {
                deliveryLat = coords[0];
                deliveryLng = coords[1];
                log.info("Geocoded delivery address '{}' -> {}, {}", event.getDeliveryAddress(), deliveryLat, deliveryLng);
            }
        }

        // === Lấy toạ độ SHOP (lấy hàng) ===
        double pickupLat = 10.8411;
        double pickupLng = 106.8427;
        String pickupAddress = "Quán - Shop ID " + event.getShopId();

        try {
            RestTemplate restTemplate = new RestTemplate();
            String shopUrl = CORE_SERVICE_URL + "/core/shops/" + event.getShopId() + "/details";
            String shopJson = restTemplate.getForObject(shopUrl, String.class);
            if (shopJson != null) {
                JsonNode data = objectMapper.readTree(shopJson);
                
                double lat = safeDouble(data, "shopLat", "latitude", "lat");
                double lng = safeDouble(data, "shopLng", "longitude", "lng");
                if (lat != 0 && lng != 0) {
                    pickupLat = lat;
                    pickupLng = lng;
                }
                
                String name = safeString(data, "shopName", "name", "ten_gian_hang");
                String address = safeString(data, "locationDetail", "address", "dia_chi");
                
                if (name != null && !name.isBlank()) {
                    pickupAddress = name;
                }
                if (address != null && !address.isBlank()) {
                    pickupAddress = (name != null && !name.isBlank()) ? name + " - " + address : address;
                }
                log.info("Got shop {} coords: {}, {} and address: {}", event.getShopId(), pickupLat, pickupLng, pickupAddress);
            }
        } catch (Exception e) {
            log.warn("Could not fetch shop {} info from core-service: {}", event.getShopId(), e.getMessage());
        }

        Delivery delivery = Delivery.builder()
                .orderId(event.getOrderId())
                .orderCode(event.getOrderCode())
                .areaId(1L)
                .status(DeliveryStatus.PENDING)
                .pickupLat(pickupLat)
                .pickupLng(pickupLng)
                .pickupAddress(pickupAddress)
                .deliveryLat(deliveryLat)
                .deliveryLng(deliveryLng)
                .deliveryAddress(deliveryAddress)
                .codAmount(event.getTotalAmount() != null ? event.getTotalAmount() : BigDecimal.ZERO)
                .confirmMethod(ConfirmMethod.OTP)
                .build();

        deliveryRepository.save(delivery);
        log.info("Created new PENDING delivery for Order {} -> delivery [{},{}] pickup [{},{}]",
                event.getOrderId(), deliveryLat, deliveryLng, pickupLat, pickupLng);
    }

    /**
     * Geocode địa chỉ sang toạ độ lat/lng dùng Vietmap Geocoding API.
     * @return double[]{lat, lng} hoặc null nếu thất bại
     */
    private double[] geocodeAddress(String address) {
        try {
            String encoded = URLEncoder.encode(address, StandardCharsets.UTF_8);
            String url = "https://maps.vietmap.vn/api/geocoding/v2/search?apikey=" + VIETMAP_API_KEY + "&text=" + encoded;
            RestTemplate restTemplate = new RestTemplate();
            String json = restTemplate.getForObject(url, String.class);
            if (json == null) return null;
            JsonNode root = objectMapper.readTree(json);
            JsonNode features = root.get("features");
            if (features != null && features.isArray() && features.size() > 0) {
                JsonNode geometry = features.get(0).get("geometry");
                if (geometry != null) {
                    JsonNode coords = geometry.get("coordinates");
                    if (coords != null && coords.size() >= 2) {
                        double lng = coords.get(0).asDouble();
                        double lat = coords.get(1).asDouble();
                        return new double[]{lat, lng};
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Geocoding failed for address '{}': {}", address, e.getMessage());
        }
        return null;
    }

    private double safeDouble(JsonNode node, String... keys) {
        for (String key : keys) {
            if (node.has(key)) {
                double v = node.get(key).asDouble(0);
                if (v != 0) return v;
            }
        }
        return 0;
    }

    private String safeString(JsonNode node, String... keys) {
        for (String key : keys) {
            if (node.has(key) && !node.get(key).isNull()) {
                String v = node.get(key).asText("").trim();
                if (!v.isEmpty()) return v;
            }
        }
        return null;
    }
}
