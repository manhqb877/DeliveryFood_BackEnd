package com.fooddelivery.payment.entity;


import com.fooddelivery.payment.enums.PaymentGateway;
import com.fooddelivery.payment.enums.TransactionStatus;
import com.fooddelivery.payment.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_transactions_order", columnList = "order_id"),
        @Index(name = "idx_transactions_user", columnList = "user_id"),
        @Index(name = "idx_transactions_gateway_id", columnList = "gateway_transaction_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Khóa chính

    @Column(name = "order_id", nullable = false)
    private Long orderId; // ID đơn hàng liên quan

    @Column(name = "user_id")
    private Long userId; // ID người dùng thực hiện giao dịch

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private TransactionType transactionType; // Loại giao dịch

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_gateway", length = 30)
    private PaymentGateway paymentGateway; // Cổng thanh toán (VNPAY, MOMO, COD, v.v.)

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount; // Số tiền giao dịch

    @Column(length = 3)
    private String currency = "VND"; // Tiền tệ (mặc định VND)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status = TransactionStatus.PENDING; // Trạng thái giao dịch

    @Column(name = "gateway_transaction_id", unique = true, length = 255)
    private String gatewayTransactionId; // Mã giao dịch trả về từ cổng (VNPay/Momo)

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "gateway_response", columnDefinition = "jsonb")
    private Map<String, Object> gatewayResponse; // Phản hồi raw từ cổng thanh toán (JSONB)

    @Column(name = "idempotency_key", unique = true, length = 64)
    private String idempotencyKey; // Chống xử lý webhook trùng lặp

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}