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
        Double rawLat = event.getDeliveryLat();
        Double rawLng = event.getDeliveryLng();

        double deliveryLat;
        double deliveryLng;

        if (rawLat != null && rawLng != null && rawLat != 0.0 && rawLng != 0.0) {
            deliveryLat = rawLat;
            deliveryLng = rawLng;
        } else {
            // Geocode địa chỉ giao hàng
            double[] coords = geocodeAddress(deliveryAddress);
            if (coords != null) {
                deliveryLat = coords[0];
                deliveryLng = coords[1];
                log.info("Geocoded delivery address '{}' -> {}, {}", deliveryAddress, deliveryLat, deliveryLng);
            } else {
                // Fallback nếu không geocode được: để gần shop
                deliveryLat = 10.8095;
                deliveryLng = 106.6262;
                log.warn("Could not geocode '{}', using default Tân Phú coordinates", deliveryAddress);
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
     * Geocode địa chỉ sang toạ độ lat/lng dùng VietMap API v3 (Autocomplete + Place Details)
     * và OpenStreetMap Nominatim làm fallback.
     * @return double[]{lat, lng} hoặc null nếu thất bại
     */
    private double[] geocodeAddress(String address) {
        if (address == null || address.isBlank()) return null;
        
        // 1. Thử VietMap Autocomplete v3 + Place Details
        try {
            String encoded = URLEncoder.encode(address.trim(), StandardCharsets.UTF_8);
            String searchUrl = "https://maps.vietmap.vn/api/autocomplete/v3?apikey=" + VIETMAP_API_KEY + "&text=" + encoded;
            RestTemplate restTemplate = new RestTemplate();
            String json = restTemplate.getForObject(searchUrl, String.class);
            if (json != null) {
                JsonNode root = objectMapper.readTree(json);
                if (root.isArray() && root.size() > 0) {
                    JsonNode first = root.get(0);
                    String refId = first.has("ref_id") ? first.get("ref_id").asText() : null;
                    if (refId != null && !refId.isBlank()) {
                        String placeUrl = "https://maps.vietmap.vn/api/place/v3?apikey=" + VIETMAP_API_KEY + "&refid=" + URLEncoder.encode(refId, StandardCharsets.UTF_8);
                        String placeJson = restTemplate.getForObject(placeUrl, String.class);
                        if (placeJson != null) {
                            JsonNode placeNode = objectMapper.readTree(placeJson);
                            if (placeNode.has("lat") && placeNode.has("lng")) {
                                double lat = placeNode.get("lat").asDouble();
                                double lng = placeNode.get("lng").asDouble();
                                if (lat != 0.0 && lng != 0.0) {
                                    log.info("VietMap v3 geocoded '{}' -> lat={}, lng={}", address, lat, lng);
                                    return new double[]{lat, lng};
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("VietMap geocoding failed for address '{}': {}", address, e.getMessage());
        }

        // 2. Fallback sang OpenStreetMap Nominatim
        try {
            String encoded = URLEncoder.encode(address.trim(), StandardCharsets.UTF_8);
            String nominatimUrl = "https://nominatim.openstreetmap.org/search?format=json&limit=1&q=" + encoded;
            RestTemplate restTemplate = new RestTemplate();
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("User-Agent", "DeliveryFoodApp/1.0");
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);
            org.springframework.http.ResponseEntity<String> resp = restTemplate.exchange(
                    nominatimUrl, org.springframework.http.HttpMethod.GET, entity, String.class);
            if (resp.getBody() != null) {
                JsonNode root = objectMapper.readTree(resp.getBody());
                if (root.isArray() && root.size() > 0) {
                    double lat = root.get(0).get("lat").asDouble();
                    double lng = root.get(0).get("lon").asDouble();
                    log.info("Nominatim fallback geocoded '{}' -> lat={}, lng={}", address, lat, lng);
                    return new double[]{lat, lng};
                }
            }
        } catch (Exception e) {
            log.warn("Nominatim fallback failed for address '{}': {}", address, e.getMessage());
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
