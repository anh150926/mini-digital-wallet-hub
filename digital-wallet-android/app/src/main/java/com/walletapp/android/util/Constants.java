package com.walletapp.android.util;

public final class Constants {
    private Constants() {}

    // Android Emulator host loopback address
    public static final String BASE_URL = "http://10.0.2.2:8080/api/v1/";

    // SharedPreferences Keys
    public static final String PREF_NAME = "wallet_secure_prefs";
    public static final String KEY_ACCESS_TOKEN = "access_token";
    public static final String KEY_REFRESH_TOKEN = "refresh_token";
    public static final String KEY_USER_ID = "user_id";
    public static final String KEY_PHONE_NUMBER = "phone_number";
    public static final String KEY_FULL_NAME = "full_name";
    public static final String KEY_WALLET_ID = "wallet_id";
    public static final String KEY_DEVICE_ID = "device_id";
    public static final String KEY_BIOMETRIC_ENABLED = "biometric_enabled";

    // Request Headers
    public static final String HEADER_AUTH = "Authorization";
    public static final String HEADER_DEVICE_ID = "X-Device-Id";
    public static final String HEADER_IDEMPOTENCY_KEY = "Idempotency-Key";
}
