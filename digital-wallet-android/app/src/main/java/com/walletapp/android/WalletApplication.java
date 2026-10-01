package com.walletapp.android;

import android.app.Application;

import com.walletapp.android.data.local.SecureStorage;
import com.walletapp.android.data.remote.ApiClient;
import com.walletapp.android.data.remote.ApiService;
import com.walletapp.android.data.repository.AuthRepository;
import com.walletapp.android.data.repository.QrRepository;
import com.walletapp.android.data.repository.TransferRepository;
import com.walletapp.android.data.repository.WalletRepository;

public class WalletApplication extends Application {

    private static WalletApplication instance;
    private SecureStorage secureStorage;
    private AuthRepository authRepository;
    private WalletRepository walletRepository;
    private TransferRepository transferRepository;
    private QrRepository qrRepository;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        // Initialize dependencies
        this.secureStorage = new SecureStorage(this);
        ApiService apiService = ApiClient.getInstance(secureStorage).getApiService();

        this.authRepository = new AuthRepository(apiService, secureStorage);
        this.walletRepository = new WalletRepository(apiService);
        this.transferRepository = new TransferRepository(apiService);
        this.qrRepository = new QrRepository(apiService);
    }

    public static WalletApplication getInstance() {
        return instance;
    }

    public SecureStorage getSecureStorage() {
        return secureStorage;
    }

    public AuthRepository getAuthRepository() {
        return authRepository;
    }

    public WalletRepository getWalletRepository() {
        return walletRepository;
    }

    public TransferRepository getTransferRepository() {
        return transferRepository;
    }

    public QrRepository getQrRepository() {
        return qrRepository;
    }
}
