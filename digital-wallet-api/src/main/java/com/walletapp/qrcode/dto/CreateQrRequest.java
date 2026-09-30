package com.walletapp.qrcode.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateQrRequest {

    private BigDecimal amount;

    private String description;

    @JsonProperty("order_reference")
    private String orderReference;

    @JsonProperty("qr_type")
    @Builder.Default
    private String qrType = "DYNAMIC";
}
