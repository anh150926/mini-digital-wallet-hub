package com.walletapp.android.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.walletapp.android.data.remote.ApiCallback;
import com.walletapp.android.data.remote.dto.PageResponse;
import com.walletapp.android.data.remote.dto.TopupResponse;
import com.walletapp.android.data.remote.dto.TransactionHistoryItem;
import com.walletapp.android.data.remote.dto.WalletResponse;
import com.walletapp.android.data.repository.TransferRepository;
import com.walletapp.android.data.repository.WalletRepository;

import java.math.BigDecimal;
import java.util.List;

public class HomeViewModel extends ViewModel {

    private final WalletRepository walletRepository;
    private final TransferRepository transferRepository;

    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<WalletResponse> walletData = new MutableLiveData<>();
    private final MutableLiveData<List<TransactionHistoryItem>> recentTransactions = new MutableLiveData<>();
    private final MutableLiveData<TopupResponse> topupSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public HomeViewModel(WalletRepository walletRepository, TransferRepository transferRepository) {
        this.walletRepository = walletRepository;
        this.transferRepository = transferRepository;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<WalletResponse> getWalletData() {
        return walletData;
    }

    public LiveData<List<TransactionHistoryItem>> getRecentTransactions() {
        return recentTransactions;
    }

    public LiveData<TopupResponse> getTopupSuccess() {
        return topupSuccess;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void loadDashboard() {
        isLoading.setValue(true);

        walletRepository.getMyWallet(new ApiCallback<WalletResponse>() {
            @Override
            public void onSuccess(WalletResponse data) {
                walletData.setValue(data);
                loadRecentTransactions();
            }

            @Override
            public void onError(String code, String message) {
                isLoading.setValue(false);
                errorMessage.setValue(message);
            }
        });
    }

    private void loadRecentTransactions() {
        walletRepository.getMyTransactions(0, 5, new ApiCallback<PageResponse<TransactionHistoryItem>>() {
            @Override
            public void onSuccess(PageResponse<TransactionHistoryItem> data) {
                isLoading.setValue(false);
                if (data != null && data.getContent() != null) {
                    recentTransactions.setValue(data.getContent());
                }
            }

            @Override
            public void onError(String code, String message) {
                isLoading.setValue(false);
            }
        });
    }

    public void topup(BigDecimal amount, String paymentMethod) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            errorMessage.setValue("Vui lòng chọn hoặc nhập số tiền hợp lệ");
            return;
        }

        isLoading.setValue(true);
        transferRepository.topup(amount, paymentMethod, new ApiCallback<TopupResponse>() {
            @Override
            public void onSuccess(TopupResponse data) {
                isLoading.setValue(false);
                topupSuccess.setValue(data);
                loadDashboard(); // Refresh balance
            }

            @Override
            public void onError(String code, String message) {
                isLoading.setValue(false);
                errorMessage.setValue(message != null ? message : "Nạp tiền thất bại");
            }
        });
    }
}
