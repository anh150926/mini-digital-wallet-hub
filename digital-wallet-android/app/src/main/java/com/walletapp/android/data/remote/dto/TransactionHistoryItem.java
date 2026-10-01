package com.walletapp.android.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;

public class TransactionHistoryItem {

    @SerializedName("transaction_id")
    private String transactionId;

    @SerializedName("type")
    private String type;

    @SerializedName("status")
    private String status;

    @SerializedName("amount")
    private BigDecimal amount;

    @SerializedName("fee")
    private BigDecimal fee;

    @SerializedName("description")
    private String description;

    @SerializedName("source_wallet_id")
    private String sourceWalletId;

    @SerializedName("dest_wallet_id")
    private String destWalletId;

    @SerializedName("created_at")
    private String createdAt;

    public String getTransactionId() {
        return transactionId;
    }

    public String getType() {
        return type;
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

    public String getDescription() {
        return description;
    }

    public String getSourceWalletId() {
        return sourceWalletId;
    }

    public String getDestWalletId() {
        return destWalletId;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
