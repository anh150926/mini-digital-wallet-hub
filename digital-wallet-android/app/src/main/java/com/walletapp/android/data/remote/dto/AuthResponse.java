package com.walletapp.android.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class AuthResponse {

    @SerializedName("access_token")
    private String accessToken;

    @SerializedName("refresh_token")
    private String refreshToken;

    @SerializedName("token_type")
    private String tokenType;

    @SerializedName("expires_in")
    private Long expiresIn;

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

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }

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
}
