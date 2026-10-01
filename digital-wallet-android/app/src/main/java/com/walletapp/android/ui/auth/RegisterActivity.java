package com.walletapp.android.ui.auth;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.walletapp.android.WalletApplication;
import com.walletapp.android.databinding.ActivityRegisterBinding;
import com.walletapp.android.ui.common.ErrorDialog;
import com.walletapp.android.ui.common.LoadingDialog;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private RegisterViewModel viewModel;
    private LoadingDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WalletApplication app = (WalletApplication) getApplication();
        viewModel = new RegisterViewModel(app.getAuthRepository());

        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        loadingDialog = new LoadingDialog(this);

        initViews();
        observeViewModel();
    }

    private void initViews() {
        binding.btnBack.setOnClickListener(v -> finish());
        binding.tvLoginLink.setOnClickListener(v -> finish());

        binding.btnRegister.setOnClickListener(v -> {
            String phone = binding.etPhone.getText() != null ? binding.etPhone.getText().toString() : "";
            String fullName = binding.etFullName.getText() != null ? binding.etFullName.getText().toString() : "";
            String password = binding.etPassword.getText() != null ? binding.etPassword.getText().toString() : "";
            String pin = binding.etPin.getText() != null ? binding.etPin.getText().toString() : "";

            viewModel.register(phone, fullName, password, pin);
        });
    }

    private void observeViewModel() {
        viewModel.getIsLoading().observe(this, loading -> {
            if (loading) {
                loadingDialog.show();
            } else {
                loadingDialog.dismiss();
            }
        });

        viewModel.getRegisterSuccess().observe(this, res -> {
            Toast.makeText(this, "Đăng ký ví thành công! Vui lòng đăng nhập", Toast.LENGTH_LONG).show();
            finish();
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                ErrorDialog.show(this, "Đăng ký thất bại", error);
            }
        });
    }
}
