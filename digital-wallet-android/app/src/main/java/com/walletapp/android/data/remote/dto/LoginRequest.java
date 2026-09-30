package com.walletapp.android.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class LoginRequest {

    @SerializedName("phone_number")
    private String phoneNumber;

    @SerializedName("password")
    private String password;

    public LoginRequest(String phoneNumber, String password) {
        this.phoneNumber = phoneNumber;
        this.password = password;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getPassword() {
        return password;
    }
}
