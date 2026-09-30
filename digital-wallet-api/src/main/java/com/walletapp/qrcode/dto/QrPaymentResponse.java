package com.walletapp.qrcode.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QrPaymentResponse {

    @JsonProperty("transaction_id")
    private UUID transactionId;

    private String status;

    private BigDecimal amount;

    @JsonProperty("recipient_name")
    private String recipientName;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;
}
