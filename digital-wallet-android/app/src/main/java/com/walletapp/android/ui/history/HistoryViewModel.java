package com.walletapp.android.ui.history;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.walletapp.android.data.remote.ApiCallback;
import com.walletapp.android.data.remote.dto.PageResponse;
import com.walletapp.android.data.remote.dto.TransactionHistoryItem;
import com.walletapp.android.data.repository.WalletRepository;

import java.util.ArrayList;
import java.util.List;

public class HistoryViewModel extends ViewModel {

    private final WalletRepository walletRepository;

    private int currentPage = 0;
    private int totalPages = 1;
    private boolean isFetching = false;

    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<List<TransactionHistoryItem>> transactions = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public HistoryViewModel(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<List<TransactionHistoryItem>> getTransactions() {
        return transactions;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void loadInitial() {
        currentPage = 0;
        isFetching = true;
        isLoading.setValue(true);

        walletRepository.getMyTransactions(0, 15, new ApiCallback<PageResponse<TransactionHistoryItem>>() {
            @Override
            public void onSuccess(PageResponse<TransactionHistoryItem> data) {
                isFetching = false;
                isLoading.setValue(false);
                if (data != null) {
                    totalPages = data.getTotalPages();
                    transactions.setValue(data.getContent() != null ? data.getContent() : new ArrayList<>());
                }
            }

            @Override
            public void onError(String code, String message) {
                isFetching = false;
                isLoading.setValue(false);
                errorMessage.setValue(message);
            }
        });
    }

    public void loadMore() {
        if (isFetching || currentPage >= totalPages - 1) {
            return;
        }

        isFetching = true;
        int nextPage = currentPage + 1;

        walletRepository.getMyTransactions(nextPage, 15, new ApiCallback<PageResponse<TransactionHistoryItem>>() {
            @Override
            public void onSuccess(PageResponse<TransactionHistoryItem> data) {
                isFetching = false;
                if (data != null && data.getContent() != null && !data.getContent().isEmpty()) {
                    currentPage = nextPage;
                    totalPages = data.getTotalPages();
                    List<TransactionHistoryItem> current = transactions.getValue() != null ? new ArrayList<>(transactions.getValue()) : new ArrayList<>();
                    current.addAll(data.getContent());
                    transactions.setValue(current);
                }
            }

            @Override
            public void onError(String code, String message) {
                isFetching = false;
            }
        });
    }
}
