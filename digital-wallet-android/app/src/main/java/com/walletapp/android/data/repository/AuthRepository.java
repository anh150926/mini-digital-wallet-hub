package com.walletapp.android.data.repository;

import androidx.annotation.NonNull;

import com.walletapp.android.data.local.SecureStorage;
import com.walletapp.android.data.remote.ApiCallback;
import com.walletapp.android.data.remote.ApiService;
import com.walletapp.android.data.remote.ApiUtils;
import com.walletapp.android.data.remote.dto.ApiResponse;
import com.walletapp.android.data.remote.dto.AuthResponse;
import com.walletapp.android.data.remote.dto.LoginRequest;
import com.walletapp.android.data.remote.dto.RegisterRequest;
import com.walletapp.android.data.remote.dto.RegisterResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthRepository {

    private final ApiService apiService;
    private final SecureStorage secureStorage;

    public AuthRepository(ApiService apiService, SecureStorage secureStorage) {
        this.apiService = apiService;
        this.secureStorage = secureStorage;
    }

    public void register(String phoneNumber, String fullName, String password, String pin, ApiCallback<RegisterResponse> callback) {
        RegisterRequest request = new RegisterRequest(phoneNumber, fullName, password, pin);
        apiService.register(request).enqueue(new Callback<ApiResponse<RegisterResponse>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<RegisterResponse>> call,
                                   @NonNull Response<ApiResponse<RegisterResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    callback.onSuccess(response.body().getData());
                } else {
                    ApiUtils.handleError(response, callback);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<RegisterResponse>> call, @NonNull Throwable t) {
                callback.onError("NETWORK_ERROR", t.getMessage() != null ? t.getMessage() : "Lỗi kết nối máy chủ");
            }
        });
    }

    public void login(String phoneNumber, String password, ApiCallback<AuthResponse> callback) {
        LoginRequest request = new LoginRequest(phoneNumber, password);
        apiService.login(request).enqueue(new Callback<ApiResponse<AuthResponse>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<AuthResponse>> call,
                                   @NonNull Response<ApiResponse<AuthResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    AuthResponse data = response.body().getData();
                    secureStorage.saveTokens(data.getAccessToken(), data.getRefreshToken());
                    secureStorage.saveUserSession(data.getUserId(), data.getPhoneNumber(), data.getFullName(), data.getWalletId());
                    callback.onSuccess(data);
                } else {
                    ApiUtils.handleError(response, callback);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<AuthResponse>> call, @NonNull Throwable t) {
                callback.onError("NETWORK_ERROR", t.getMessage() != null ? t.getMessage() : "Lỗi kết nối máy chủ");
            }
        });
    }

    public boolean isLoggedIn() {
        return secureStorage.isLoggedIn();
    }

    public void logout() {
        secureStorage.clear();
    }

    public SecureStorage getSecureStorage() {
        return secureStorage;
    }
}
