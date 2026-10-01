package com.walletapp.android.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class RegisterResponse {

    @SerializedName("user_id")
    private String userId;

    @SerializedName("wallet_id")
    private String walletId;

    @SerializedName("phone_number")
    private String phoneNumber;

    @SerializedName("full_name")
    private String fullName;

    @SerializedName("role")
    private String role;

    @SerializedName("created_at")
    private String createdAt;

    public String getUserId() {
        return userId;
    }

    public String getWalletId() {
        return walletId;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getFullName() {
        return fullName;
    }

    public String getRole() {
        return role;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
