package com.walletapp.android.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;

public class TopupRequest {

    @SerializedName("amount")
    private BigDecimal amount;

    @SerializedName("payment_method")
    private String paymentMethod;

    public TopupRequest(BigDecimal amount, String paymentMethod) {
        this.amount = amount;
        this.paymentMethod = paymentMethod;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }
}
