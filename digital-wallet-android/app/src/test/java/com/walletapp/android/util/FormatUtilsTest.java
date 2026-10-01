package com.walletapp.android.util;

import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class FormatUtilsTest {

    @Test
    public void testFormatVnd() {
        assertEquals("50,000 đ", FormatUtils.formatVnd(new BigDecimal("50000")));
        assertEquals("1,000,000 đ", FormatUtils.formatVnd(1000000));
        assertEquals("0 đ", FormatUtils.formatVnd((BigDecimal) null));
    }

    @Test
    public void testFormatIsoDateTime() {
        String iso = "2026-10-01T14:30:00+07:00";
        String formatted = FormatUtils.formatIsoDateTime(iso);
        assertNotNull(formatted);
        assertEquals("14:30 - 01/10/2026", formatted);
    }
}
