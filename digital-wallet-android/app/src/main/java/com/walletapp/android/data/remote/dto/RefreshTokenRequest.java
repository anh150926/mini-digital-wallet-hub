package com.walletapp.android.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class RefreshTokenRequest {

    @SerializedName("refresh_token")
    private String refreshToken;

    public RefreshTokenRequest(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }
}
