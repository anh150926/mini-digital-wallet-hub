package com.walletapp.transaction.entity;

import com.walletapp.common.enums.TransactionStatus;
import com.walletapp.common.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "idempotency_key", length = 64, unique = true, nullable = false)
    private String idempotencyKey;

    @Column(name = "source_wallet_id")
    private UUID sourceWalletId;

    @Column(name = "dest_wallet_id")
    private UUID destWalletId;

    @Column(name = "amount", precision = 19, scale = 0, nullable = false)
    private BigDecimal amount;

    @Column(name = "fee", precision = 19, scale = 0, nullable = false)
    @Builder.Default
    private BigDecimal fee = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 30, nullable = false)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.PENDING;

    @Column(name = "request_hash", length = 64)
    private String requestHash;

    @Column(name = "request_signature", columnDefinition = "TEXT")
    private String requestSignature;

    @Column(name = "error_code", length = 50)
    private String errorCode;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
