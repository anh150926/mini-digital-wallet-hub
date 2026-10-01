package com.walletapp.concurrency;

import com.walletapp.auth.dto.AuthResponse;
import com.walletapp.auth.dto.LoginRequest;
import com.walletapp.auth.dto.RegisterRequest;
import com.walletapp.auth.dto.RegisterResponse;
import com.walletapp.auth.service.AuthService;
import com.walletapp.wallet.entity.Wallet;
import com.walletapp.wallet.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
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
class IdempotencyTamperingTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private AuthService authService;

    @Autowired
    private WalletRepository walletRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("Kịch bản 3: Idempotency Tampering - Chặn request cùng key nhưng payload bị thay đổi (HTTP 422 IDEMPOTENCY_PAYLOAD_MISMATCH)")
    void testIdempotencyTampering() throws Exception {
        long ts = System.currentTimeMillis() % 100000000L;
        String phoneA = "09" + String.format("%08d", ts);
        String phoneB = "09" + String.format("%08d", ts + 1);

        RegisterResponse regA = authService.register(RegisterRequest.builder()
                .phoneNumber(phoneA)
                .fullName("User Idempotency A")
                .password("Password@123")
                .pin("123456")
                .build());

        authService.register(RegisterRequest.builder()
                .phoneNumber(phoneB)
                .fullName("User Idempotency B")
                .password("Password@123")
                .pin("123456")
                .build());

        AuthResponse loginA = authService.login(LoginRequest.builder()
                .phoneNumber(phoneA)
                .password("Password@123")
                .build());
        String tokenA = loginA.getAccessToken();

        // Nạp 200.000 VNĐ cho A
        mockMvc.perform(post("/api/v1/topup")
                .header("Authorization", "Bearer " + tokenA)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":200000,\"payment_method\":\"SIMULATED_BANK\"}")).andExpect(status().isOk());

        String sharedKey = UUID.randomUUID().toString();
        String validPayload = String.format(
                "{\"dest_phone_number\":\"%s\",\"amount\":50000,\"description\":\"Giao dich hop le\",\"pin\":\"123456\"}",
                phoneB
        );

        // 1. Gửi request lần đầu -> 200 OK, trừ 50.000 còn 150.000
        mockMvc.perform(post("/api/v1/transfers")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("Idempotency-Key", sharedKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.source_balance_after").value(150000));

        // 2. Gửi lại request với cùng key và cùng payload -> 200 OK (cached response), số dư không đổi
        mockMvc.perform(post("/api/v1/transfers")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("Idempotency-Key", sharedKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.source_balance_after").value(150000));

        Wallet walletAAfterCached = walletRepository.findById(regA.getWalletId()).orElseThrow();
        assertThat(walletAAfterCached.getBalance()).isEqualByComparingTo(new BigDecimal("150000"));

        // 3. Gửi request cùng key nhưng payload bị thay đổi (amount đổi thành 100.000) -> 422 IDEMPOTENCY_PAYLOAD_MISMATCH
        String tamperedPayload = String.format(
                "{\"dest_phone_number\":\"%s\",\"amount\":100000,\"description\":\"Hacked payload\",\"pin\":\"123456\"}",
                phoneB
        );

        mockMvc.perform(post("/api/v1/transfers")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("Idempotency-Key", sharedKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tamperedPayload))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("IDEMPOTENCY_PAYLOAD_MISMATCH"));

        // Số dư ví A vẫn giữ nguyên 150.000 VNĐ
        Wallet walletAFinal = walletRepository.findById(regA.getWalletId()).orElseThrow();
        assertThat(walletAFinal.getBalance()).isEqualByComparingTo(new BigDecimal("150000"));
    }
}
