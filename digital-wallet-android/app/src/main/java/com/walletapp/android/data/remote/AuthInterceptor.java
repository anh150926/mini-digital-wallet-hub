package com.walletapp.android.data.remote;

import androidx.annotation.NonNull;

import com.walletapp.android.data.local.SecureStorage;
import com.walletapp.android.util.Constants;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthInterceptor implements Interceptor {

    private final SecureStorage secureStorage;

    public AuthInterceptor(SecureStorage secureStorage) {
        this.secureStorage = secureStorage;
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request original = chain.request();
        Request.Builder builder = original.newBuilder();

        builder.header("Accept", "application/json");

        String deviceId = secureStorage.getOrCreateDeviceId();
        if (deviceId != null) {
            builder.header(Constants.HEADER_DEVICE_ID, deviceId);
        }

        String token = secureStorage.getAccessToken();
        if (token != null && !token.isEmpty() && original.header(Constants.HEADER_AUTH) == null) {
            builder.header(Constants.HEADER_AUTH, "Bearer " + token);
        }

        return chain.proceed(builder.build());
    }
}
