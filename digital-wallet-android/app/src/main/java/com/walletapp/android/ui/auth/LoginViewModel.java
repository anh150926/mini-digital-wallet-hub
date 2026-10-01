package com.walletapp.android.ui.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.walletapp.android.data.remote.ApiCallback;
import com.walletapp.android.data.remote.dto.AuthResponse;
import com.walletapp.android.data.repository.AuthRepository;

public class LoginViewModel extends ViewModel {

    private final AuthRepository authRepository;

    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<AuthResponse> loginSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public LoginViewModel(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<AuthResponse> getLoginSuccess() {
        return loginSuccess;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void login(String phoneNumber, String password) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            errorMessage.setValue("Vui lòng nhập số điện thoại");
            return;
        }
        if (password == null || password.trim().isEmpty()) {
            errorMessage.setValue("Vui lòng nhập mật khẩu");
            return;
        }

        isLoading.setValue(true);
        authRepository.login(phoneNumber.trim(), password, new ApiCallback<AuthResponse>() {
            @Override
            public void onSuccess(AuthResponse data) {
                isLoading.setValue(false);
                loginSuccess.setValue(data);
            }

            @Override
            public void onError(String code, String message) {
                isLoading.setValue(false);
                errorMessage.setValue(message != null ? message : "Đăng nhập thất bại");
            }
        });
    }

    public boolean isLoggedIn() {
        return authRepository.isLoggedIn();
    }
}
