package com.walletapp.android.data.remote;

import com.walletapp.android.data.remote.dto.ApiResponse;
import com.walletapp.android.data.remote.dto.AuthResponse;
import com.walletapp.android.data.remote.dto.CreateQrRequest;
import com.walletapp.android.data.remote.dto.LoginRequest;
import com.walletapp.android.data.remote.dto.PageResponse;
import com.walletapp.android.data.remote.dto.QrCodeResponse;
import com.walletapp.android.data.remote.dto.QrPaymentRequest;
import com.walletapp.android.data.remote.dto.QrPaymentResponse;
import com.walletapp.android.data.remote.dto.RefreshTokenRequest;
import com.walletapp.android.data.remote.dto.RegisterRequest;
import com.walletapp.android.data.remote.dto.RegisterResponse;
import com.walletapp.android.data.remote.dto.TopupRequest;
import com.walletapp.android.data.remote.dto.TopupResponse;
import com.walletapp.android.data.remote.dto.TransactionHistoryItem;
import com.walletapp.android.data.remote.dto.TransferRequest;
import com.walletapp.android.data.remote.dto.TransferResponse;
import com.walletapp.android.data.remote.dto.WalletResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiService {

    @POST("auth/register")
    Call<ApiResponse<RegisterResponse>> register(@Body RegisterRequest request);

    @POST("auth/login")
    Call<ApiResponse<AuthResponse>> login(@Body LoginRequest request);

    @POST("auth/refresh")
    Call<ApiResponse<AuthResponse>> refreshToken(@Body RefreshTokenRequest request);

    @GET("wallets/me")
    Call<ApiResponse<WalletResponse>> getMyWallet();

    @GET("wallets/me/transactions")
    Call<ApiResponse<PageResponse<TransactionHistoryItem>>> getMyTransactions(
            @Query("page") int page,
            @Query("size") int size
    );

    @POST("transfers")
    Call<ApiResponse<TransferResponse>> transfer(
            @Header("Idempotency-Key") String idempotencyKey,
            @Body TransferRequest request
    );

    @POST("topup")
    Call<ApiResponse<TopupResponse>> topup(
            @Header("Idempotency-Key") String idempotencyKey,
            @Body TopupRequest request
    );

    @POST("qr-codes")
    Call<ApiResponse<QrCodeResponse>> createQrCode(@Body CreateQrRequest request);

    @POST("qr-codes/pay")
    Call<ApiResponse<QrPaymentResponse>> payQrCode(
            @Header("Idempotency-Key") String idempotencyKey,
            @Body QrPaymentRequest request
    );
}
