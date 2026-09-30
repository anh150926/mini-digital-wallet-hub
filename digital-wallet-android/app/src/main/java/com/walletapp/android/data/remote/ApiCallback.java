package com.walletapp.android.data.remote;

public interface ApiCallback<T> {
    void onSuccess(T data);
    void onError(String code, String message);
}
