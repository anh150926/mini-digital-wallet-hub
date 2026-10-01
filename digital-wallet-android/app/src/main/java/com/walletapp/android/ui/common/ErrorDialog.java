package com.walletapp.android.ui.common;

import android.content.Context;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.walletapp.android.R;

public class ErrorDialog {

    public static void show(Context context, String title, String message) {
        new MaterialAlertDialogBuilder(context)
                .setTitle(title != null ? title : "Thông báo")
                .setMessage(message != null ? message : "Đã xảy ra sự cố")
                .setPositiveButton(R.string.close, (dialog, which) -> dialog.dismiss())
                .show();
    }

    public static void show(Context context, String message) {
        show(context, "Lỗi giao dịch", message);
    }
}
