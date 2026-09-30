package com.walletapp.android.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;

public class QrPaymentResponse {

    @SerializedName("transaction_id")
    private String transactionId;

    @SerializedName("status")
    private String status;

    @SerializedName("amount")
    private BigDecimal amount;

    @SerializedName("recipient_name")
    private String recipientName;

    @SerializedName("created_at")
    private String createdAt;

    public String getTransactionId() {
        return transactionId;
    }

    public String getStatus() {
        return status;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
