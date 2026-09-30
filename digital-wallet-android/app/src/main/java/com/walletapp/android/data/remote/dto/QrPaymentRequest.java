package com.walletapp.android.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class QrPaymentRequest {

    @SerializedName("qr_payload")
    private String qrPayload;

    @SerializedName("pin")
    private String pin;

    public QrPaymentRequest(String qrPayload, String pin) {
        this.qrPayload = qrPayload;
        this.pin = pin;
    }

    public String getQrPayload() {
        return qrPayload;
    }

    public String getPin() {
        return pin;
    }
}
