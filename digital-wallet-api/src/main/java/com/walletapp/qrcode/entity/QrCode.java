package com.walletapp.qrcode.entity;

import com.walletapp.common.enums.QrType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "qr_codes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QrCode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "wallet_id", nullable = false)
    private UUID walletId;

    @Column(name = "amount", precision = 19, scale = 0)
    private BigDecimal amount;

    @Column(name = "order_reference", length = 50, unique = true)
    private String orderReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "qr_type", length = 10, nullable = false)
    @Builder.Default
    private QrType qrType = QrType.DYNAMIC;

    @Column(name = "payload", columnDefinition = "TEXT", nullable = false)
    private String payload;

    @Column(name = "expiry_time")
    private OffsetDateTime expiryTime;

    @Column(name = "is_used", nullable = false)
    @Builder.Default
    private Boolean isUsed = false;

    @Column(name = "transaction_id")
    private UUID transactionId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
