package com.walletapp.transaction.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferRequest {

    @NotBlank(message = "Số điện thoại người nhận không được để trống")
    @JsonProperty("dest_phone_number")
    private String destPhoneNumber;

    @NotNull(message = "Số tiền không được để trống")
    @DecimalMin(value = "10000", message = "Số tiền chuyển tối thiểu là 10.000 VNĐ")
    private BigDecimal amount;

    private String description;

    private String pin;

    @JsonProperty("pin_hash")
    private String pinHash;

    private String nonce;

    private Long timestamp;

    private String signature;
}
