package com.walletapp.android.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;

public class TransferRequest {

    @SerializedName("dest_phone_number")
    private String destPhoneNumber;

    @SerializedName("amount")
    private BigDecimal amount;

    @SerializedName("description")
    private String description;

    @SerializedName("pin")
    private String pin;

    public TransferRequest(String destPhoneNumber, BigDecimal amount, String description, String pin) {
        this.destPhoneNumber = destPhoneNumber;
        this.amount = amount;
        this.description = description;
        this.pin = pin;
    }

    public String getDestPhoneNumber() {
        return destPhoneNumber;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getDescription() {
        return description;
    }

    public String getPin() {
        return pin;
    }
}
