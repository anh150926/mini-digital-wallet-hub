package com.walletapp.android.data.repository;

import androidx.annotation.NonNull;

import com.walletapp.android.data.remote.ApiCallback;
import com.walletapp.android.data.remote.ApiService;
import com.walletapp.android.data.remote.ApiUtils;
import com.walletapp.android.data.remote.dto.ApiResponse;
import com.walletapp.android.data.remote.dto.CreateQrRequest;
import com.walletapp.android.data.remote.dto.QrCodeResponse;
import com.walletapp.android.data.remote.dto.QrPaymentRequest;
import com.walletapp.android.data.remote.dto.QrPaymentResponse;

import java.math.BigDecimal;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class QrRepository {

    private final ApiService apiService;

    public QrRepository(ApiService apiService) {
        this.apiService = apiService;
    }

    public void createQrCode(BigDecimal amount, String description, String orderReference, String qrType,
                             ApiCallback<QrCodeResponse> callback) {
        CreateQrRequest request = new CreateQrRequest(amount, description, orderReference, qrType);
        apiService.createQrCode(request).enqueue(new Callback<ApiResponse<QrCodeResponse>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<QrCodeResponse>> call,
                                   @NonNull Response<ApiResponse<QrCodeResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    callback.onSuccess(response.body().getData());
                } else {
                    ApiUtils.handleError(response, callback);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<QrCodeResponse>> call, @NonNull Throwable t) {
                callback.onError("NETWORK_ERROR", t.getMessage() != null ? t.getMessage() : "Lỗi kết nối máy chủ");
            }
        });
    }

    public void payQrCode(String qrPayload, String pin, ApiCallback<QrPaymentResponse> callback) {
        String idempotencyKey = UUID.randomUUID().toString();
        QrPaymentRequest request = new QrPaymentRequest(qrPayload, pin);

        apiService.payQrCode(idempotencyKey, request).enqueue(new Callback<ApiResponse<QrPaymentResponse>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<QrPaymentResponse>> call,
                                   @NonNull Response<ApiResponse<QrPaymentResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    callback.onSuccess(response.body().getData());
                } else {
                    ApiUtils.handleError(response, callback);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<QrPaymentResponse>> call, @NonNull Throwable t) {
                callback.onError("NETWORK_ERROR", t.getMessage() != null ? t.getMessage() : "Lỗi kết nối máy chủ");
            }
        });
    }
}
