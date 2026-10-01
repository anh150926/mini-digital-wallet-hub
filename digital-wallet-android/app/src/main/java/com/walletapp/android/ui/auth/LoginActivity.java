package com.walletapp.android.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.walletapp.android.MainActivity;
import com.walletapp.android.WalletApplication;
import com.walletapp.android.databinding.ActivityLoginBinding;
import com.walletapp.android.ui.common.ErrorDialog;
import com.walletapp.android.ui.common.LoadingDialog;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private LoginViewModel viewModel;
    private LoadingDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WalletApplication app = (WalletApplication) getApplication();
        viewModel = new LoginViewModel(app.getAuthRepository());

        // Check if user already logged in
        if (viewModel.isLoggedIn()) {
            navigateToMain();
            return;
        }

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        loadingDialog = new LoadingDialog(this);

        initViews();
        observeViewModel();
    }

    private void initViews() {
        binding.btnLogin.setOnClickListener(v -> {
            String phone = binding.etPhone.getText() != null ? binding.etPhone.getText().toString() : "";
            String password = binding.etPassword.getText() != null ? binding.etPassword.getText().toString() : "";
            viewModel.login(phone, password);
        });

        binding.tvRegisterLink.setOnClickListener(v -> {
            Intent intent = new Intent(this, RegisterActivity.class);
            startActivity(intent);
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

        viewModel.getLoginSuccess().observe(this, authResponse -> {
            Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
            navigateToMain();
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                ErrorDialog.show(this, "Đăng nhập thất bại", error);
            }
        });
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
