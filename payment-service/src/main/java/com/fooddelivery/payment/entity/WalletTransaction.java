package com.fooddelivery.payment.entity;

import com.fooddelivery.payment.enums.WalletTxType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "wallet_transactions", indexes = {
        @Index(name = "idx_wallet_tx_wallet", columnList = "wallet_id, created_at DESC")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Khóa chính

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet; // Ví liên quan

    @Column(name = "order_id")
    private Long orderId; // Đơn hàng liên quan (nếu có)

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount; // Số tiền biến động (Dương = nhận, Âm = trừ)

    @Column(name = "balance_before", nullable = false, precision = 12, scale = 2)
    private BigDecimal balanceBefore; // Số dư trước giao dịch

    @Column(name = "balance_after", nullable = false, precision = 12, scale = 2)
    private BigDecimal balanceAfter; // Số dư sau giao dịch

    @Enumerated(EnumType.STRING)
    @Column(name = "tx_type", nullable = false, length = 20)
    private WalletTxType txType; // Loại giao dịch (DEBIT, CREDIT, REFUND)

    @Column(columnDefinition = "TEXT")
    private String description; // Mô tả giao dịch

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}