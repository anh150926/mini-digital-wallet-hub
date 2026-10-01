package com.walletapp.android.util;

import com.walletapp.android.domain.model.QrCodeInfo;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public final class VietQrParser {
    private VietQrParser() {}

    public static QrCodeInfo parse(String rawPayload) {
        if (rawPayload == null || !rawPayload.startsWith("000201")) {
            return new QrCodeInfo(rawPayload, null, null, null, null, null, false, false);
        }

        boolean validCrc = Crc16Calculator.validateChecksum(rawPayload);
        if (!validCrc) {
            return new QrCodeInfo(rawPayload, null, null, null, null, null, false, false);
        }

        Map<String, String> rootTags = parseTlv(rawPayload);

        String poiMethod = rootTags.get("01"); // 11: Static, 12: Dynamic
        boolean isDynamic = "12".equals(poiMethod);

        String acquirerId = null;
        String walletId = null;
        String tag38Raw = rootTags.get("38");
        if (tag38Raw != null) {
            Map<String, String> nested38 = parseTlv(tag38Raw);
            acquirerId = nested38.get("00");
            walletId = nested38.get("01");
        }

        BigDecimal amount = null;
        String amountStr = rootTags.get("54");
        if (amountStr != null) {
            try {
                amount = new BigDecimal(amountStr);
            } catch (Exception ignored) {}
        }

        String description = null;
        String tag62Raw = rootTags.get("62");
        if (tag62Raw != null) {
            Map<String, String> nested62 = parseTlv(tag62Raw);
            description = nested62.get("08");
        }

        return new QrCodeInfo(
                rawPayload, poiMethod, acquirerId, walletId, amount, description,
                isDynamic, true
        );
    }

    public static Map<String, String> parseTlv(String payload) {
        Map<String, String> result = new HashMap<>();
        int i = 0;
        int len = payload.length();

        while (i + 4 <= len) {
            String tag = payload.substring(i, i + 2);
            int tagLength;
            try {
                tagLength = Integer.parseInt(payload.substring(i + 2, i + 4));
            } catch (NumberFormatException e) {
                break;
            }

            int valStart = i + 4;
            int valEnd = valStart + tagLength;
            if (valEnd > len) {
                break;
            }

            String value = payload.substring(valStart, valEnd);
            result.put(tag, value);
            i = valEnd;
        }

        return result;
    }
}
