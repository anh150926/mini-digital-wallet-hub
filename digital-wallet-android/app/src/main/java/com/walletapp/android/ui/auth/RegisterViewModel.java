package com.walletapp.android.ui.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.walletapp.android.data.remote.ApiCallback;
import com.walletapp.android.data.remote.dto.RegisterResponse;
import com.walletapp.android.data.repository.AuthRepository;

public class RegisterViewModel extends ViewModel {

    private final AuthRepository authRepository;

    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<RegisterResponse> registerSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public RegisterViewModel(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<RegisterResponse> getRegisterSuccess() {
        return registerSuccess;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void register(String phoneNumber, String fullName, String password, String pin) {
        if (phoneNumber == null || phoneNumber.trim().length() < 9) {
            errorMessage.setValue("Vui lòng nhập số điện thoại hợp lệ");
            return;
        }
        if (fullName == null || fullName.trim().isEmpty()) {
            errorMessage.setValue("Vui lòng nhập họ và tên");
            return;
        }
        if (password == null || password.length() < 6) {
            errorMessage.setValue("Mật khẩu phải có tối thiểu 6 ký tự");
            return;
        }
        if (pin == null || pin.length() != 6 || !pin.matches("\\d{6}")) {
            errorMessage.setValue("Mã PIN phải bao gồm đúng 6 chữ số");
            return;
        }

        isLoading.setValue(true);
        authRepository.register(phoneNumber.trim(), fullName.trim(), password, pin, new ApiCallback<RegisterResponse>() {
            @Override
            public void onSuccess(RegisterResponse data) {
                isLoading.setValue(false);
                registerSuccess.setValue(data);
            }

            @Override
            public void onError(String code, String message) {
                isLoading.setValue(false);
                errorMessage.setValue(message != null ? message : "Đăng ký thất bại");
            }
        });
    }
}
