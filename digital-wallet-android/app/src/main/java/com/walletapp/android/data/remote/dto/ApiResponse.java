package com.walletapp.android.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

public class ApiResponse<T> {

    @SerializedName("success")
    private boolean success;

    @SerializedName("data")
    private T data;

    @SerializedName("error")
    private ApiError error;

    @SerializedName("timestamp")
    private String timestamp;

    public boolean isSuccess() {
        return success;
    }

    public T getData() {
        return data;
    }

    public ApiError getError() {
        return error;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public static class ApiError {
        @SerializedName("code")
        private String code;

        @SerializedName("message")
        private String message;

        @SerializedName("details")
        private Map<String, Object> details;

        public String getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }

        public Map<String, Object> getDetails() {
            return details;
        }
    }
}
