package com.walletapp.common.util;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public final class VietQrUtils {

    public static final String GUID_NAPAS = "A000000727";
    public static final String SERVICE_CODE_ACCOUNT = "QRIBFTTA";
    public static final String DEFAULT_BIN = "970400";
    public static final String CURRENCY_VND = "704";
    public static final String COUNTRY_VN = "VN";

    private VietQrUtils() {}

    public static String formatTlv(String tag, String value) {
        if (value == null) {
            return "";
        }
        int length = value.getBytes(StandardCharsets.UTF_8).length;
        return String.format("%s%02d%s", tag, length, value);
    }

    public static String generateVietQrPayload(
            String beneficiaryWalletId,
            String bin,
            BigDecimal amount,
            String description,
            boolean isDynamic
    ) {
        StringBuilder sb = new StringBuilder();

        // Tag 00: Payload Format Indicator
        sb.append(formatTlv("00", "01"));

        // Tag 01: Point of Initiation Method (11: Static, 12: Dynamic)
        sb.append(formatTlv("01", isDynamic ? "12" : "11"));

        // Tag 38: Merchant Account Information (VietQR NAPAS)
        String sub00 = formatTlv("00", GUID_NAPAS);
        String subSub00 = formatTlv("00", bin != null ? bin : DEFAULT_BIN);
        String subSub01 = formatTlv("01", beneficiaryWalletId);
        String sub01 = formatTlv("01", subSub00 + subSub01);
        String sub02 = formatTlv("02", SERVICE_CODE_ACCOUNT);
        sb.append(formatTlv("38", sub00 + sub01 + sub02));

        // Tag 53: Transaction Currency (704 = VND)
        sb.append(formatTlv("53", CURRENCY_VND));

        // Tag 54: Transaction Amount
        if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
            sb.append(formatTlv("54", amount.toPlainString()));
        }

        // Tag 58: Country Code
        sb.append(formatTlv("58", COUNTRY_VN));

        // Tag 62: Additional Data Field (Purpose/Description)
        if (description != null && !description.isBlank()) {
            String sub08 = formatTlv("08", description);
            sb.append(formatTlv("62", sub08));
        }

        // Tag 63: CRC16 Checksum
        sb.append("6304");
        String crc = calculateCrc16(sb.toString());
        sb.append(crc);

        return sb.toString();
    }

    public static String calculateCrc16(String data) {
        int crc = 0xFFFF;
        int polynomial = 0x1021;
        byte[] bytes = data.getBytes(StandardCharsets.US_ASCII);

        for (byte b : bytes) {
            for (int i = 0; i < 8; i++) {
                boolean bit = ((b >> (7 - i)) & 1) == 1;
                boolean c15 = ((crc >> 15) & 1) == 1;
                crc <<= 1;
                if (c15 ^ bit) {
                    crc ^= polynomial;
                }
            }
        }
        crc &= 0xFFFF;
        return String.format("%04X", crc);
    }

    public static boolean validateCrc16(String qrPayload) {
        if (qrPayload == null || qrPayload.length() < 8) {
            return false;
        }

        // Expected format ends with 6304XXXX
        int tag63Index = qrPayload.lastIndexOf("6304");
        if (tag63Index == -1 || tag63Index != qrPayload.length() - 8) {
            return false;
        }

        String dataWithoutCrc = qrPayload.substring(0, tag63Index + 4);
        String providedCrc = qrPayload.substring(tag63Index + 4);
        String expectedCrc = calculateCrc16(dataWithoutCrc);

        return expectedCrc.equalsIgnoreCase(providedCrc);
    }

    public static Map<String, String> parseTlv(String payload) {
        Map<String, String> tags = new HashMap<>();
        if (payload == null || payload.length() < 4) {
            return tags;
        }

        int index = 0;
        int totalLen = payload.length();

        while (index + 4 <= totalLen) {
            String tag = payload.substring(index, index + 2);
            int len;
            try {
                len = Integer.parseInt(payload.substring(index + 2, index + 4));
            } catch (NumberFormatException e) {
                break;
            }

            index += 4;
            if (index + len > totalLen) {
                break;
            }

            String value = payload.substring(index, index + len);
            tags.put(tag, value);
            index += len;
        }

        return tags;
    }

    public static String extractWalletId(String qrPayload) {
        Map<String, String> rootTags = parseTlv(qrPayload);
        String tag38 = rootTags.get("38");
        if (tag38 == null) {
            return null;
        }

        Map<String, String> subTags = parseTlv(tag38);
        String sub01 = subTags.get("01");
        if (sub01 == null) {
            return null;
        }

        Map<String, String> subSubTags = parseTlv(sub01);
        return subSubTags.get("01");
    }

    public static BigDecimal extractAmount(String qrPayload) {
        Map<String, String> rootTags = parseTlv(qrPayload);
        String amountStr = rootTags.get("54");
        if (amountStr == null || amountStr.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(amountStr);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
