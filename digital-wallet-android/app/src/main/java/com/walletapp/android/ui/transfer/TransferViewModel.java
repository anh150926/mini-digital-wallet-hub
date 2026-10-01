package com.walletapp.android.ui.transfer;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.walletapp.android.data.remote.ApiCallback;
import com.walletapp.android.data.remote.dto.TransferResponse;
import com.walletapp.android.data.repository.TransferRepository;

import java.math.BigDecimal;

public class TransferViewModel extends ViewModel {

    private final TransferRepository transferRepository;

    private String destPhoneNumber;
    private BigDecimal amount;
    private String description;

    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<TransferResponse> transferSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public TransferViewModel(TransferRepository transferRepository) {
        this.transferRepository = transferRepository;
    }

    public void setTransferData(String destPhoneNumber, BigDecimal amount, String description) {
        this.destPhoneNumber = destPhoneNumber;
        this.amount = amount;
        this.description = description;
    }

    public String getDestPhoneNumber() {
        return destPhoneNumber;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getDescription() {
        return description;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<TransferResponse> getTransferSuccess() {
        return transferSuccess;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void executeTransfer(String pin) {
        if (destPhoneNumber == null || destPhoneNumber.trim().isEmpty()) {
            errorMessage.setValue("Thiếu thông tin người nhận");
            return;
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            errorMessage.setValue("Số tiền chuyển không hợp lệ");
            return;
        }
        if (pin == null || pin.length() != 6) {
            errorMessage.setValue("Mã PIN phải bao gồm 6 chữ số");
            return;
        }

        isLoading.setValue(true);
        transferRepository.transfer(destPhoneNumber, amount, description, pin, new ApiCallback<TransferResponse>() {
            @Override
            public void onSuccess(TransferResponse data) {
                isLoading.setValue(false);
                transferSuccess.setValue(data);
            }

            @Override
            public void onError(String code, String message) {
                isLoading.setValue(false);
                errorMessage.setValue(message != null ? message : "Chuyển tiền thất bại");
            }
        });
    }
}
