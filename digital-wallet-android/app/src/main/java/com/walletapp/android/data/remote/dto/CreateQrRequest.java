package com.walletapp.android.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;

public class CreateQrRequest {

    @SerializedName("amount")
    private BigDecimal amount;

    @SerializedName("description")
    private String description;

    @SerializedName("order_reference")
    private String orderReference;

    @SerializedName("qr_type")
    private String qrType; // "DYNAMIC" or "STATIC"

    public CreateQrRequest(BigDecimal amount, String description, String orderReference, String qrType) {
        this.amount = amount;
        this.description = description;
        this.orderReference = orderReference;
        this.qrType = qrType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getDescription() {
        return description;
    }

    public String getOrderReference() {
        return orderReference;
    }

    public String getQrType() {
        return qrType;
    }
}
