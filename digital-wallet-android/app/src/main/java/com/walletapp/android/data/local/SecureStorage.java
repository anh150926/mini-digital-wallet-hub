package com.walletapp.android.data.local;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.walletapp.android.util.Constants;

import java.util.UUID;

public class SecureStorage {
    private static final String TAG = "SecureStorage";
    private SharedPreferences prefs;

    public SecureStorage(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            this.prefs = EncryptedSharedPreferences.create(
                    context,
                    Constants.PREF_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize EncryptedSharedPreferences, fallback to standard prefs", e);
            this.prefs = context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE);
        }
    }

    public void saveTokens(String accessToken, String refreshToken) {
        prefs.edit()
                .putString(Constants.KEY_ACCESS_TOKEN, accessToken)
                .putString(Constants.KEY_REFRESH_TOKEN, refreshToken)
                .apply();
    }

    public String getAccessToken() {
        return prefs.getString(Constants.KEY_ACCESS_TOKEN, null);
    }

    public String getRefreshToken() {
        return prefs.getString(Constants.KEY_REFRESH_TOKEN, null);
    }

    public void saveUserSession(String userId, String phoneNumber, String fullName, String walletId) {
        prefs.edit()
                .putString(Constants.KEY_USER_ID, userId)
                .putString(Constants.KEY_PHONE_NUMBER, phoneNumber)
                .putString(Constants.KEY_FULL_NAME, fullName)
                .putString(Constants.KEY_WALLET_ID, walletId)
                .apply();
    }

    public String getUserId() {
        return prefs.getString(Constants.KEY_USER_ID, null);
    }

    public String getPhoneNumber() {
        return prefs.getString(Constants.KEY_PHONE_NUMBER, null);
    }

    public String getFullName() {
        return prefs.getString(Constants.KEY_FULL_NAME, null);
    }

    public String getWalletId() {
        return prefs.getString(Constants.KEY_WALLET_ID, null);
    }

    public String getOrCreateDeviceId() {
        String deviceId = prefs.getString(Constants.KEY_DEVICE_ID, null);
        if (deviceId == null) {
            deviceId = UUID.randomUUID().toString();
            prefs.edit().putString(Constants.KEY_DEVICE_ID, deviceId).apply();
        }
        return deviceId;
    }

    public boolean isBiometricEnabled() {
        return prefs.getBoolean(Constants.KEY_BIOMETRIC_ENABLED, false);
    }

    public void setBiometricEnabled(boolean enabled) {
        prefs.edit().putBoolean(Constants.KEY_BIOMETRIC_ENABLED, enabled).apply();
    }

    public boolean isLoggedIn() {
        return getAccessToken() != null;
    }

    public void clear() {
        String deviceId = prefs.getString(Constants.KEY_DEVICE_ID, null);
        prefs.edit().clear().apply();
        if (deviceId != null) {
            prefs.edit().putString(Constants.KEY_DEVICE_ID, deviceId).apply();
        }
    }
}
