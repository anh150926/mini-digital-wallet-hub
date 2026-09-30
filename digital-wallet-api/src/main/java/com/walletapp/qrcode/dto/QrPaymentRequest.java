package com.walletapp.qrcode.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QrPaymentRequest {

    @NotBlank(message = "Chuỗi QR payload không được để trống")
    @JsonProperty("qr_payload")
    private String qrPayload;

    private String pin;

    @JsonProperty("pin_hash")
    private String pinHash;

    private String nonce;

    private Long timestamp;

    private String signature;
}
