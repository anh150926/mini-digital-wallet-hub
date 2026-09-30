package com.walletapp.android.data.remote;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.walletapp.android.data.local.SecureStorage;
import com.walletapp.android.data.remote.dto.ApiResponse;
import com.walletapp.android.data.remote.dto.AuthResponse;
import com.walletapp.android.data.remote.dto.RefreshTokenRequest;
import com.walletapp.android.util.Constants;

import java.io.IOException;

import okhttp3.Authenticator;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.Route;

public class TokenRefreshAuthenticator implements Authenticator {

    private final SecureStorage secureStorage;
    private final Gson gson = new Gson();

    public TokenRefreshAuthenticator(SecureStorage secureStorage) {
        this.secureStorage = secureStorage;
    }

    @Nullable
    @Override
    public Request authenticate(@Nullable Route route, @NonNull Response response) throws IOException {
        // Prevent infinite loops if refresh fails
        if (responseCount(response) >= 3) {
            secureStorage.clear();
            return null;
        }

        String refreshToken = secureStorage.getRefreshToken();
        if (refreshToken == null || refreshToken.isEmpty()) {
            secureStorage.clear();
            return null;
        }

        synchronized (this) {
            String currentToken = secureStorage.getAccessToken();
            String headerToken = response.request().header(Constants.HEADER_AUTH);
            if (headerToken != null && headerToken.startsWith("Bearer ") && currentToken != null) {
                String existingToken = headerToken.substring(7);
                if (!existingToken.equals(currentToken)) {
                    // Token was already refreshed by another thread
                    return response.request().newBuilder()
                            .header(Constants.HEADER_AUTH, "Bearer " + currentToken)
                            .build();
                }
            }

            // Perform synchronous refresh call
            OkHttpClient client = new OkHttpClient.Builder().build();
            String jsonBody = gson.toJson(new RefreshTokenRequest(refreshToken));
            RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json"));

            Request refreshRequest = new Request.Builder()
                    .url(Constants.BASE_URL + "auth/refresh")
                    .post(body)
                    .build();

            try (Response refreshResponse = client.newCall(refreshRequest).execute()) {
                if (refreshResponse.isSuccessful() && refreshResponse.body() != null) {
                    String responseStr = refreshResponse.body().string();
                    java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<ApiResponse<AuthResponse>>() {}.getType();
                    ApiResponse<AuthResponse> apiResponse = gson.fromJson(responseStr, type);

                    if (apiResponse != null && apiResponse.isSuccess() && apiResponse.getData() != null) {
                        String newAccessToken = apiResponse.getData().getAccessToken();
                        String newRefreshToken = apiResponse.getData().getRefreshToken();
                        secureStorage.saveTokens(newAccessToken, newRefreshToken);

                        return response.request().newBuilder()
                                .header(Constants.HEADER_AUTH, "Bearer " + newAccessToken)
                                .build();
                    }
                }
            } catch (Exception ignored) {}

            secureStorage.clear();
            return null;
        }
    }

    private int responseCount(Response response) {
        int result = 1;
        while ((response = response.priorResponse()) != null) {
            result++;
        }
        return result;
    }
}
