package com.walletapp.android.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;

public class TopupResponse {

    @SerializedName("transaction_id")
    private String transactionId;

    @SerializedName("status")
    private String status;

    @SerializedName("new_balance")
    private BigDecimal newBalance;

    public String getTransactionId() {
        return transactionId;
    }

    public String getStatus() {
        return status;
    }

    public BigDecimal getNewBalance() {
        return newBalance;
    }
}
