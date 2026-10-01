package com.walletapp.android.data.repository;

import androidx.annotation.NonNull;

import com.walletapp.android.data.remote.ApiCallback;
import com.walletapp.android.data.remote.ApiService;
import com.walletapp.android.data.remote.ApiUtils;
import com.walletapp.android.data.remote.dto.ApiResponse;
import com.walletapp.android.data.remote.dto.TopupRequest;
import com.walletapp.android.data.remote.dto.TopupResponse;
import com.walletapp.android.data.remote.dto.TransferRequest;
import com.walletapp.android.data.remote.dto.TransferResponse;

import java.math.BigDecimal;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TransferRepository {

    private final ApiService apiService;

    public TransferRepository(ApiService apiService) {
        this.apiService = apiService;
    }

    public void transfer(String destPhoneNumber, BigDecimal amount, String description, String pin,
                         ApiCallback<TransferResponse> callback) {
        String idempotencyKey = UUID.randomUUID().toString();
        TransferRequest request = new TransferRequest(destPhoneNumber, amount, description, pin);

        apiService.transfer(idempotencyKey, request).enqueue(new Callback<ApiResponse<TransferResponse>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<TransferResponse>> call,
                                   @NonNull Response<ApiResponse<TransferResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    callback.onSuccess(response.body().getData());
                } else {
                    ApiUtils.handleError(response, callback);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<TransferResponse>> call, @NonNull Throwable t) {
                callback.onError("NETWORK_ERROR", t.getMessage() != null ? t.getMessage() : "Lỗi kết nối máy chủ");
            }
        });
    }

    public void topup(BigDecimal amount, String paymentMethod, ApiCallback<TopupResponse> callback) {
        String idempotencyKey = UUID.randomUUID().toString();
        TopupRequest request = new TopupRequest(amount, paymentMethod);

        apiService.topup(idempotencyKey, request).enqueue(new Callback<ApiResponse<TopupResponse>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<TopupResponse>> call,
                                   @NonNull Response<ApiResponse<TopupResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    callback.onSuccess(response.body().getData());
                } else {
                    ApiUtils.handleError(response, callback);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<TopupResponse>> call, @NonNull Throwable t) {
                callback.onError("NETWORK_ERROR", t.getMessage() != null ? t.getMessage() : "Lỗi kết nối máy chủ");
            }
        });
    }
}
