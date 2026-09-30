package com.walletapp.qrcode;

import com.jayway.jsonpath.JsonPath;
import com.walletapp.auth.dto.AuthResponse;
import com.walletapp.auth.dto.LoginRequest;
import com.walletapp.auth.dto.RegisterRequest;
import com.walletapp.auth.dto.RegisterResponse;
import com.walletapp.auth.service.AuthService;
import com.walletapp.common.util.VietQrUtils;
import com.walletapp.qrcode.entity.QrCode;
import com.walletapp.qrcode.repository.QrCodeRepository;
import com.walletapp.wallet.entity.Wallet;
import com.walletapp.wallet.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = com.walletapp.DigitalWalletApiApplication.class)
class QrCodeIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private AuthService authService;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private QrCodeRepository qrCodeRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("Comprehensive VietQR Flow: EMVCo Generation, CRC16 Validation, Payment & Single-Use Enforcement")
    void testVietQrFlow() throws Exception {
        long ts = System.currentTimeMillis() % 100000000L;
        String phoneA = "09" + String.format("%08d", ts);
        String phoneB = "09" + String.format("%08d", ts + 1);

        // 1. Register User A (Payer) & User B (Payee/Merchant)
        RegisterResponse regA = authService.register(RegisterRequest.builder()
                .phoneNumber(phoneA)
                .fullName("Nguyen Van Payer")
                .password("SecurePass123!")
                .pin("123456")
                .build());

        RegisterResponse regB = authService.register(RegisterRequest.builder()
                .phoneNumber(phoneB)
                .fullName("Tran Thi Merchant")
                .password("SecurePass123!")
                .pin("654321")
                .build());

        AuthResponse loginA = authService.login(LoginRequest.builder()
                .phoneNumber(phoneA)
                .password("SecurePass123!")
                .build());
        String tokenA = loginA.getAccessToken();

        AuthResponse loginB = authService.login(LoginRequest.builder()
                .phoneNumber(phoneB)
                .password("SecurePass123!")
                .build());
        String tokenB = loginB.getAccessToken();

        // 2. User B creates a Dynamic VietQR code for 250,000 VND
        String orderRef = "ORDER-" + UUID.randomUUID().toString().substring(0, 8);
        String createQrJson = String.format("{\"amount\":250000,\"description\":\"Cơm trưa văn phòng\",\"order_reference\":\"%s\",\"qr_type\":\"DYNAMIC\"}", orderRef);

        MvcResult qrResult = mockMvc.perform(post("/api/v1/qr-codes")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createQrJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.qr_type").value("DYNAMIC"))
                .andExpect(jsonPath("$.data.amount").value(250000))
                .andExpect(jsonPath("$.data.expiry_time").isNotEmpty())
                .andReturn();

        String qrResponseBody = qrResult.getResponse().getContentAsString();
        String qrPayload = JsonPath.read(qrResponseBody, "$.data.payload");
        String qrIdStr = JsonPath.read(qrResponseBody, "$.data.qr_id");
        UUID qrId = UUID.fromString(qrIdStr);

        // 3. Verify EMVCo TLV format and CRC16 checksum
        assertThat(VietQrUtils.validateCrc16(qrPayload)).isTrue();
        assertThat(VietQrUtils.extractWalletId(qrPayload)).isEqualTo(regB.getWalletId().toString());
        assertThat(VietQrUtils.extractAmount(qrPayload)).isEqualByComparingTo(new BigDecimal("250000"));

        // 4. User A tops up 1,000,000 VND
        String topupKey = UUID.randomUUID().toString();
        mockMvc.perform(post("/api/v1/topup")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("Idempotency-Key", topupKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":1000000,\"payment_method\":\"SIMULATED_BANK\"}"))
                .andExpect(status().isOk());

        // 5. User A pays the VietQR code
        String payKey = UUID.randomUUID().toString();
        String payJson = String.format(
                "{\"qr_payload\":\"%s\",\"pin\":\"123456\"}",
                qrPayload
        );

        mockMvc.perform(post("/api/v1/qr-codes/pay")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("Idempotency-Key", payKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.amount").value(250000))
                .andExpect(jsonPath("$.data.recipient_name").value("Tran Thi Merchant"));

        // 6. Check wallet balances
        Wallet walletA = walletRepository.findById(regA.getWalletId()).orElseThrow();
        Wallet walletB = walletRepository.findById(regB.getWalletId()).orElseThrow();
        assertThat(walletA.getBalance()).isEqualByComparingTo(new BigDecimal("750000"));
        assertThat(walletB.getBalance()).isEqualByComparingTo(new BigDecimal("250000"));

        // 7. Check QR code status in DB (BR-SPEC-QR02)
        QrCode savedQr = qrCodeRepository.findById(qrId).orElseThrow();
        assertThat(savedQr.getIsUsed()).isTrue();
        assertThat(savedQr.getTransactionId()).isNotNull();

        // 8. User A tries to pay the SAME dynamic QR code again -> 410 QR_ALREADY_USED
        String payKey2 = UUID.randomUUID().toString();
        mockMvc.perform(post("/api/v1/qr-codes/pay")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("Idempotency-Key", payKey2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payJson))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.error.code").value("QR_ALREADY_USED"));

        // 9. Tampered QR code payload (wrong CRC16) -> 422 QR_INVALID_CHECKSUM (BR-SPEC-QR03)
        String corruptedPayload = qrPayload.substring(0, qrPayload.length() - 4) + "0000";
        String payKey3 = UUID.randomUUID().toString();
        String corruptedPayJson = String.format(
                "{\"qr_payload\":\"%s\",\"pin\":\"123456\"}",
                corruptedPayload
        );
        mockMvc.perform(post("/api/v1/qr-codes/pay")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("Idempotency-Key", payKey3)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corruptedPayJson))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("QR_INVALID_CHECKSUM"));
    }
}
