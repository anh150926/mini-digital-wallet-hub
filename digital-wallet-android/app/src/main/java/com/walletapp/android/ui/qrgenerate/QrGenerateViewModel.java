package com.walletapp.android.ui.qrgenerate;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.CountDownTimer;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.walletapp.android.data.remote.ApiCallback;
import com.walletapp.android.data.remote.dto.QrCodeResponse;
import com.walletapp.android.data.repository.QrRepository;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class QrGenerateViewModel extends ViewModel {

    private final QrRepository qrRepository;

    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<QrCodeResponse> qrResponse = new MutableLiveData<>();
    private final MutableLiveData<Bitmap> qrBitmap = new MutableLiveData<>();
    private final MutableLiveData<String> remainingTimeText = new MutableLiveData<>("");
    private final MutableLiveData<Boolean> isExpired = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    private CountDownTimer countDownTimer;

    public QrGenerateViewModel(QrRepository qrRepository) {
        this.qrRepository = qrRepository;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<QrCodeResponse> getQrResponse() {
        return qrResponse;
    }

    public LiveData<Bitmap> getQrBitmap() {
        return qrBitmap;
    }

    public LiveData<String> getRemainingTimeText() {
        return remainingTimeText;
    }

    public LiveData<Boolean> getIsExpired() {
        return isExpired;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void generateQr(BigDecimal amount, String description) {
        String orderRef = "ORDER-" + UUID.randomUUID().toString().substring(0, 8);
        String qrType = (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) ? "DYNAMIC" : "STATIC";

        isLoading.setValue(true);
        qrRepository.createQrCode(amount, description, orderRef, qrType, new ApiCallback<QrCodeResponse>() {
            @Override
            public void onSuccess(QrCodeResponse data) {
                isLoading.setValue(false);
                qrResponse.setValue(data);

                // Generate QR Bitmap
                Bitmap bitmap = generateQrBitmap(data.getPayload(), 512, 512);
                qrBitmap.setValue(bitmap);

                // Start 15-min countdown for dynamic QR
                if ("DYNAMIC".equalsIgnoreCase(data.getQrType())) {
                    startCountdown(15 * 60 * 1000);
                } else {
                    remainingTimeText.setValue("Mã tĩnh (Không giới hạn thời gian)");
                    isExpired.setValue(false);
                }
            }

            @Override
            public void onError(String code, String message) {
                isLoading.setValue(false);
                errorMessage.setValue(message != null ? message : "Tạo mã QR thất bại");
            }
        });
    }

    private void startCountdown(long durationMs) {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        isExpired.setValue(false);

        countDownTimer = new CountDownTimer(durationMs, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long minutes = (millisUntilFinished / 1000) / 60;
                long seconds = (millisUntilFinished / 1000) % 60;
                remainingTimeText.setValue(String.format(Locale.getDefault(), "Mã hết hạn sau: %02d:%02d", minutes, seconds));
            }

            @Override
            public void onFinish() {
                remainingTimeText.setValue("Mã QR đã hết hạn");
                isExpired.setValue(true);
            }
        }.start();
    }

    private Bitmap generateQrBitmap(String content, int width, int height) {
        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1);

            BitMatrix bitMatrix = new MultiFormatWriter().encode(
                    content, BarcodeFormat.QR_CODE, width, height, hints
            );

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            return bitmap;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}
