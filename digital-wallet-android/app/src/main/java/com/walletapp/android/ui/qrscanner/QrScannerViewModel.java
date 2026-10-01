package com.walletapp.android.ui.qrscanner;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.walletapp.android.data.remote.ApiCallback;
import com.walletapp.android.data.remote.dto.QrPaymentResponse;
import com.walletapp.android.data.remote.dto.TransferResponse;
import com.walletapp.android.data.repository.QrRepository;
import com.walletapp.android.domain.model.QrCodeInfo;

public class QrScannerViewModel extends ViewModel {

    private final QrRepository qrRepository;

    private QrCodeInfo scannedQrInfo;

    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<QrPaymentResponse> paymentSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public QrScannerViewModel(QrRepository qrRepository) {
        this.qrRepository = qrRepository;
    }

    public void setScannedQrInfo(QrCodeInfo qrInfo) {
        this.scannedQrInfo = qrInfo;
    }

    public QrCodeInfo getScannedQrInfo() {
        return scannedQrInfo;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<QrPaymentResponse> getPaymentSuccess() {
        return paymentSuccess;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void payQr(String pin) {
        if (scannedQrInfo == null || scannedQrInfo.getRawPayload() == null) {
            errorMessage.setValue("Không tìm thấy thông tin mã QR");
            return;
        }
        if (pin == null || pin.length() != 6) {
            errorMessage.setValue("Mã PIN phải bao gồm 6 chữ số");
            return;
        }

        isLoading.setValue(true);
        qrRepository.payQrCode(scannedQrInfo.getRawPayload(), pin, new ApiCallback<QrPaymentResponse>() {
            @Override
            public void onSuccess(QrPaymentResponse data) {
                isLoading.setValue(false);
                paymentSuccess.setValue(data);
            }

            @Override
            public void onError(String code, String message) {
                isLoading.setValue(false);
                errorMessage.setValue(message != null ? message : "Thanh toán QR thất bại");
            }
        });
    }
}
