package com.walletapp.android.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;

public class WalletResponse {

    @SerializedName("wallet_id")
    private String walletId;

    @SerializedName("user_id")
    private String userId;

    @SerializedName("phone_number")
    private String phoneNumber;

    @SerializedName("full_name")
    private String fullName;

    @SerializedName("balance")
    private BigDecimal balance;

    @SerializedName("currency")
    private String currency;

    @SerializedName("status")
    private String status;

    public String getWalletId() {
        return walletId;
    }

    public String getUserId() {
        return userId;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getFullName() {
        return fullName;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public String getCurrency() {
        return currency;
    }

    public String getStatus() {
        return status;
    }
}
