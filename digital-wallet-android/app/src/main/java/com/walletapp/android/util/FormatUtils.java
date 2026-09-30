package com.walletapp.android.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class FormatUtils {
    private FormatUtils() {}

    private static final DecimalFormatSymbols SYMBOLS = new DecimalFormatSymbols(Locale.US);
    static {
        SYMBOLS.setGroupingSeparator(',');
        SYMBOLS.setDecimalSeparator('.');
    }
    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,###", SYMBOLS);
    private static final DateTimeFormatter DISPLAY_DATE_FORMAT = DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy");

    public static String formatVnd(BigDecimal amount) {
        if (amount == null) {
            return "0 đ";
        }
        return MONEY_FORMAT.format(amount) + " đ";
    }

    public static String formatVnd(long amount) {
        return formatVnd(BigDecimal.valueOf(amount));
    }

    public static String formatIsoDateTime(String isoString) {
        if (isoString == null || isoString.isEmpty()) {
            return "";
        }
        try {
            OffsetDateTime odt = OffsetDateTime.parse(isoString);
            return odt.format(DISPLAY_DATE_FORMAT);
        } catch (Exception e) {
            return isoString;
        }
    }
}
