package com.walletapp.android.data.repository;

import androidx.annotation.NonNull;

import com.walletapp.android.data.remote.ApiCallback;
import com.walletapp.android.data.remote.ApiService;
import com.walletapp.android.data.remote.ApiUtils;
import com.walletapp.android.data.remote.dto.ApiResponse;
import com.walletapp.android.data.remote.dto.PageResponse;
import com.walletapp.android.data.remote.dto.TransactionHistoryItem;
import com.walletapp.android.data.remote.dto.WalletResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WalletRepository {

    private final ApiService apiService;

    public WalletRepository(ApiService apiService) {
        this.apiService = apiService;
    }

    public void getMyWallet(ApiCallback<WalletResponse> callback) {
        apiService.getMyWallet().enqueue(new Callback<ApiResponse<WalletResponse>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<WalletResponse>> call,
                                   @NonNull Response<ApiResponse<WalletResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    callback.onSuccess(response.body().getData());
                } else {
                    ApiUtils.handleError(response, callback);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<WalletResponse>> call, @NonNull Throwable t) {
                callback.onError("NETWORK_ERROR", t.getMessage() != null ? t.getMessage() : "Lỗi kết nối máy chủ");
            }
        });
    }

    public void getMyTransactions(int page, int size, ApiCallback<PageResponse<TransactionHistoryItem>> callback) {
        apiService.getMyTransactions(page, size).enqueue(new Callback<ApiResponse<PageResponse<TransactionHistoryItem>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<PageResponse<TransactionHistoryItem>>> call,
                                   @NonNull Response<ApiResponse<PageResponse<TransactionHistoryItem>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    callback.onSuccess(response.body().getData());
                } else {
                    ApiUtils.handleError(response, callback);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<PageResponse<TransactionHistoryItem>>> call, @NonNull Throwable t) {
                callback.onError("NETWORK_ERROR", t.getMessage() != null ? t.getMessage() : "Lỗi kết nối máy chủ");
            }
        });
    }
}
