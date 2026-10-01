package com.walletapp.android.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class RegisterRequest {

    @SerializedName("phone_number")
    private String phoneNumber;

    @SerializedName("full_name")
    private String fullName;

    @SerializedName("password")
    private String password;

    @SerializedName("pin")
    private String pin;

    public RegisterRequest(String phoneNumber, String fullName, String password, String pin) {
        this.phoneNumber = phoneNumber;
        this.fullName = fullName;
        this.password = password;
        this.pin = pin;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPassword() {
        return password;
    }

    public String getPin() {
        return pin;
    }
}
