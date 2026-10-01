package com.walletapp.android.data.remote;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.walletapp.android.data.remote.dto.ApiResponse;

import java.lang.reflect.Type;

import okhttp3.ResponseBody;
import retrofit2.Response;

public final class ApiUtils {
    private ApiUtils() {}

    private static final Gson GSON = new Gson();

    public static <T> void handleError(Response<T> response, ApiCallback<?> callback) {
        String code = "UNKNOWN_ERROR";
        String message = "Đã xảy ra lỗi không xác định";

        ResponseBody errorBody = response.errorBody();
        if (errorBody != null) {
            try {
                String errorJson = errorBody.string();
                Type type = new TypeToken<ApiResponse<Object>>() {}.getType();
                ApiResponse<Object> apiResponse = GSON.fromJson(errorJson, type);
                if (apiResponse != null && apiResponse.getError() != null) {
                    if (apiResponse.getError().getCode() != null) {
                        code = apiResponse.getError().getCode();
                    }
                    if (apiResponse.getError().getMessage() != null) {
                        message = apiResponse.getError().getMessage();
                    }
                }
            } catch (Exception ignored) {}
        }

        callback.onError(code, message);
    }
}
