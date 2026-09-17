package com.fooddelivery.payment.entity;

import com.fooddelivery.payment.enums.WalletStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "wallets")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Khóa chính

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId; // ID khách hàng sở hữu ví (1 user = 1 ví duy nhất)

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO; // Số dư ví (có thể âm nếu đang ghi nhận nợ)

    @Column(name = "credit_limit", nullable = false, precision = 12, scale = 2)
    private BigDecimal creditLimit = BigDecimal.ZERO; // Hạn mức tín dụng được Admin cấp

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WalletStatus status = WalletStatus.ACTIVE; // Trạng thái ví (ACTIVE, FROZEN, CLOSED)

    @Column(name = "last_transaction_at")
    private OffsetDateTime lastTransactionAt; // Thời điểm phát sinh giao dịch ví gần nhất

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}