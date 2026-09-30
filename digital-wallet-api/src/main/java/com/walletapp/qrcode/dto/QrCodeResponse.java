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
public class QrCodeResponse {

    @JsonProperty("qr_id")
    private UUID qrId;

    private String payload;

    @JsonProperty("qr_type")
    private String qrType;

    private BigDecimal amount;

    @JsonProperty("expiry_time")
    private OffsetDateTime expiryTime;
}
