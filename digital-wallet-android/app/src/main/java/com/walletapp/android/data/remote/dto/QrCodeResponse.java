package com.walletapp.android.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;

public class QrCodeResponse {

    @SerializedName("qr_id")
    private String qrId;

    @SerializedName("payload")
    private String payload;

    @SerializedName("qr_type")
    private String qrType;

    @SerializedName("amount")
    private BigDecimal amount;

    @SerializedName("expiry_time")
    private String expiryTime;

    public String getQrId() {
        return qrId;
    }

    public String getPayload() {
        return payload;
    }

    public String getQrType() {
        return qrType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getExpiryTime() {
        return expiryTime;
    }
}
