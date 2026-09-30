package com.walletapp.android.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;

public class TransferResponse {

    @SerializedName("transaction_id")
    private String transactionId;

    @SerializedName("status")
    private String status;

    @SerializedName("amount")
    private BigDecimal amount;

    @SerializedName("fee")
    private BigDecimal fee;

    @SerializedName("source_balance_after")
    private BigDecimal sourceBalanceAfter;

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

    public BigDecimal getFee() {
        return fee;
    }

    public BigDecimal getSourceBalanceAfter() {
        return sourceBalanceAfter;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
