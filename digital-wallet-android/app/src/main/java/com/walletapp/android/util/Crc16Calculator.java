package com.walletapp.android.util;

public final class Crc16Calculator {
    private Crc16Calculator() {}

    private static final int POLYNOMIAL = 0x1021;
    private static final int INITIAL_VALUE = 0xFFFF;

    public static String calculateCrc16(String data) {
        int crc = INITIAL_VALUE;
        byte[] bytes = data.getBytes(java.nio.charset.StandardCharsets.US_ASCII);

        for (byte b : bytes) {
            crc ^= (b & 0xFF) << 8;
            for (int i = 0; i < 8; i++) {
                if ((crc & 0x8000) != 0) {
                    crc = ((crc << 1) ^ POLYNOMIAL) & 0xFFFF;
                } else {
                    crc = (crc << 1) & 0xFFFF;
                }
            }
        }

        return String.format("%04X", crc);
    }

    public static boolean validateChecksum(String payload) {
        if (payload == null || payload.length() < 8) {
            return false;
        }

        int crcTagIndex = payload.lastIndexOf("6304");
        if (crcTagIndex != payload.length() - 8) {
            return false;
        }

        String dataBeforeChecksum = payload.substring(0, payload.length() - 4);
        String expectedChecksum = payload.substring(payload.length() - 4).toUpperCase();
        String calculatedChecksum = calculateCrc16(dataBeforeChecksum);

        return calculatedChecksum.equalsIgnoreCase(expectedChecksum);
    }
}
