package com.fooddelivery.notification.kafka;

import com.fooddelivery.notification.document.NotificationLogDocument;
import com.fooddelivery.notification.dto.event.OrderEvent;
import com.fooddelivery.notification.enums.NotificationChannel;
import com.fooddelivery.notification.enums.NotificationStatus;
import com.fooddelivery.notification.enums.NotificationType;
import com.fooddelivery.notification.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final NotificationLogRepository notificationLogRepository;

    @KafkaListener(topics = "order-events", groupId = "notification-service-group")
    public void consumeOrderEvent(OrderEvent event) {
        if (event == null) {
            log.warn("Received empty or null OrderEvent from Kafka");
            return;
        }

        log.info("Received Kafka OrderEvent: type={}, orderId={}, code={}", 
                event.getEventType(), event.getOrderId(), event.getOrderCode());

        try {
            if ("ORDER_CREATED".equalsIgnoreCase(event.getEventType())) {
                handleOrderCreated(event);
            } else if ("ORDER_STATUS_CHANGED".equalsIgnoreCase(event.getEventType())) {
                handleOrderStatusChanged(event);
            } else {
                log.info("Unhandled event type: {}", event.getEventType());
            }
        } catch (Exception e) {
            log.error("Error processing OrderEvent: {}", e.getMessage(), e);
        }
    }

    private void handleOrderCreated(OrderEvent event) {
        String formattedPrice = formatCurrency(event.getTotalAmount());

        // 1. Gửi thông báo cho Người mua (Customer)
        if (event.getUserId() != null || event.getCustomerPhone() != null) {
            Map<String, Object> data = new HashMap<>();
            data.put("orderId", event.getOrderId());
            data.put("orderCode", event.getOrderCode());
            data.put("shopId", event.getShopId());
            data.put("totalAmount", event.getTotalAmount());
            data.put("action", "VIEW_ORDER");

            NotificationLogDocument customerNotification = NotificationLogDocument.builder()
                    .recipientId(event.getUserId())
                    .recipientPhone(event.getCustomerPhone())
                    .channel(NotificationChannel.IN_APP)
                    .notificationType(NotificationType.ORDER_STATUS)
                    .title("Đặt hàng thành công")
                    .body("Đơn hàng #" + event.getOrderCode() + " (" + formattedPrice + ") đã được gửi tới quán.")
                    .data(data)
                    .status(NotificationStatus.SENT)
                    .referenceId(event.getOrderId())
                    .createdAt(Instant.now())
                    .build();

            notificationLogRepository.save(customerNotification);
            log.info("Created customer notification for order #{}", event.getOrderCode());
        }

        // 2. Gửi thông báo cho Gian hàng (Shop)
        if (event.getShopId() != null) {
            Map<String, Object> shopData = new HashMap<>();
            shopData.put("orderId", event.getOrderId());
            shopData.put("orderCode", event.getOrderCode());
            shopData.put("type", "NEW_ORDER");
            shopData.put("totalAmount", event.getTotalAmount());

            String customerLabel = event.getCustomerName() != null && !event.getCustomerName().isBlank()
                    ? event.getCustomerName()
                    : "Khách hàng";

            NotificationLogDocument shopNotification = NotificationLogDocument.builder()
                    .recipientId(event.getShopId())
                    .channel(NotificationChannel.IN_APP)
                    .notificationType(NotificationType.ORDER_STATUS)
                    .title("Đơn hàng mới #" + event.getOrderCode())
                    .body(customerLabel + " vừa đặt đơn trị giá " + formattedPrice + ". Vui lòng xác nhận đơn!")
                    .data(shopData)
                    .status(NotificationStatus.SENT)
                    .referenceId(event.getOrderId())
                    .createdAt(Instant.now())
                    .build();

            notificationLogRepository.save(shopNotification);
            log.info("Created shop notification for order #{} (shopId={})", event.getOrderCode(), event.getShopId());
        }
    }

    private void handleOrderStatusChanged(OrderEvent event) {
        if (event.getUserId() == null && event.getCustomerPhone() == null) {
            return;
        }

        String status = event.getOrderStatus() != null ? event.getOrderStatus().toUpperCase() : "";
        String title;
        String body;

        switch (status) {
            case "CONFIRMED":
                title = "Quán đã nhận đơn";
                body = "Đơn hàng #" + event.getOrderCode() + " đang được quán chuẩn bị.";
                break;
            case "PREPARING":
                title = "Đang chuẩn bị món";
                body = "Quán đang nấu các món trong đơn hàng #" + event.getOrderCode() + ".";
                break;
            case "READY_FOR_PICKUP":
                title = "Món ăn đã hoàn tất";
                body = "Đơn hàng #" + event.getOrderCode() + " đã sẵn sàng giao cho tài xế.";
                break;
            case "DELIVERING":
                title = "Đơn hàng đang được giao";
                body = "Tài xế đang vận chuyển đơn hàng #" + event.getOrderCode() + " đến bạn.";
                break;
            case "DELIVERED":
            case "COMPLETED":
                title = "Giao hàng thành công";
                body = "Đơn hàng #" + event.getOrderCode() + " đã giao thành công. Chúc bạn ngon miệng!";
                break;
            case "CANCELLED":
                title = "Đơn hàng đã bị hủy";
                body = "Đơn hàng #" + event.getOrderCode() + " đã bị hủy." + 
                       (event.getCancelReason() != null && !event.getCancelReason().isBlank() 
                               ? " Lý do: " + event.getCancelReason() : "");
                break;
            default:
                title = "Cập nhật đơn hàng";
                body = "Đơn hàng #" + event.getOrderCode() + " đã cập nhật trạng thái: " + status;
                break;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("orderId", event.getOrderId());
        data.put("orderCode", event.getOrderCode());
        data.put("status", status);

        NotificationLogDocument statusNotification = NotificationLogDocument.builder()
                .recipientId(event.getUserId())
                .recipientPhone(event.getCustomerPhone())
                .channel(NotificationChannel.IN_APP)
                .notificationType(NotificationType.ORDER_STATUS)
                .title(title)
                .body(body)
                .data(data)
                .status(NotificationStatus.SENT)
                .referenceId(event.getOrderId())
                .createdAt(Instant.now())
                .build();

        notificationLogRepository.save(statusNotification);
        log.info("Created status update notification ({}) for order #{}", status, event.getOrderCode());
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0₫";
        DecimalFormat df = new DecimalFormat("#,###");
        return df.format(amount) + "₫";
    }
}
