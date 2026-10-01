package com.walletapp.android.util;

import com.walletapp.android.domain.model.QrCodeInfo;

import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.*;

public class VietQrParserTest {

    @Test
    public void testParseDynamicVietQr() {
        // Tag 00: 01 (Payload format)
        // Tag 01: 12 (Dynamic QR)
        // Tag 38: Sub00 (A000000727), Sub01 (123e4567-e89b-12d3-a456-426614174000), Sub02 (QRIBFTTA)
        // Tag 53: 704 (VND)
        // Tag 54: 250000 (Amount)
        // Tag 58: VN (Country)
        // Tag 62: Sub08 (Com trua)
        // Tag 63: CRC
        String payloadWithoutCrc = "00020101021238660010A0000007270136123e4567-e89b-12d3-a456-4266141740000208QRIBFTTA530370454062500005802VN62120808Com trua6304";
        String checksum = Crc16Calculator.calculateCrc16(payloadWithoutCrc);
        String fullPayload = payloadWithoutCrc + checksum;

        QrCodeInfo info = VietQrParser.parse(fullPayload);

        assertTrue("Payload should be valid", info.isValid());
        assertTrue("Should be dynamic QR", info.isDynamic());
        assertEquals("123e4567-e89b-12d3-a456-426614174000", info.getWalletId());
        assertEquals("A000000727", info.getAcquirerId());
        assertNotNull(info.getAmount());
        assertEquals(0, new BigDecimal("250000").compareTo(info.getAmount()));
        assertEquals("Com trua", info.getDescription());
    }

    @Test
    public void testParseStaticVietQr() {
        String payloadWithoutCrc = "00020101021138660010A0000007270136123e4567-e89b-12d3-a456-4266141740000208QRIBFTTA53037045802VN6304";
        String checksum = Crc16Calculator.calculateCrc16(payloadWithoutCrc);
        String fullPayload = payloadWithoutCrc + checksum;

        QrCodeInfo info = VietQrParser.parse(fullPayload);

        assertTrue("Payload should be valid", info.isValid());
        assertFalse("Should be static QR", info.isDynamic());
        assertEquals("123e4567-e89b-12d3-a456-426614174000", info.getWalletId());
        assertNull("Amount should be null for static QR", info.getAmount());
    }

    @Test
    public void testCorruptedCrcRejected() {
        String payloadWithoutCrc = "00020101021238660010A0000007270136123e4567-e89b-12d3-a456-4266141740000208QRIBFTTA530370454062500005802VN62120808Com trua6304";
        String invalidPayload = payloadWithoutCrc + "0000";

        QrCodeInfo info = VietQrParser.parse(invalidPayload);

        assertFalse("Tampered payload must be rejected", info.isValid());
    }

    @Test
    public void testNonVietQrRejected() {
        String randomQr = "https://example.com/pay?amount=50000";
        QrCodeInfo info = VietQrParser.parse(randomQr);

        assertFalse("Non-VietQR format must be rejected", info.isValid());
    }
}
