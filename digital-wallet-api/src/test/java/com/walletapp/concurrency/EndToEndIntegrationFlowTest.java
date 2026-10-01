package com.walletapp.concurrency;

import com.jayway.jsonpath.JsonPath;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = com.walletapp.DigitalWalletApiApplication.class)
class EndToEndIntegrationFlowTest {

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
    @DisplayName("Kịch bản 4: Full End-to-End System LifeCycle (Register -> Topup -> P2P -> VietQR -> Admin Audit & Freeze -> Blocked)")
    void testEndToEndFlow() throws Exception {
        long ts = System.currentTimeMillis() % 100000000L;
        String phoneA = "09" + String.format("%08d", ts);
        String phoneB = "09" + String.format("%08d", ts + 1);

        // --- BƯỚC 1: Đăng ký & Đăng nhập Người dùng A và B ---
        RegisterResponse regA = authService.register(RegisterRequest.builder()
                .phoneNumber(phoneA)
                .fullName("Nguyen Van A")
                .password("Pass@123456")
                .pin("123456")
                .build());

        RegisterResponse regB = authService.register(RegisterRequest.builder()
                .phoneNumber(phoneB)
                .fullName("Tran Van B")
                .password("Pass@123456")
                .pin("654321")
                .build());

        AuthResponse loginA = authService.login(LoginRequest.builder().phoneNumber(phoneA).password("Pass@123456").build());
        String tokenA = loginA.getAccessToken();

        AuthResponse loginB = authService.login(LoginRequest.builder().phoneNumber(phoneB).password("Pass@123456").build());
        String tokenB = loginB.getAccessToken();

        // --- BƯỚC 2: User A Nạp 500.000 VNĐ vào ví ---
        mockMvc.perform(post("/api/v1/topup")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":500000,\"payment_method\":\"SIMULATED_BANK\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.new_balance").value(500000));

        // --- BƯỚC 3: User A Chuyển 100.000 VNĐ cho User B (P2P Transfer) ---
        String p2pJson = String.format(
                "{\"dest_phone_number\":\"%s\",\"amount\":100000,\"description\":\"Tien ca phe\",\"pin\":\"123456\"}",
                phoneB
        );
        mockMvc.perform(post("/api/v1/transfers")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(p2pJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.source_balance_after").value(400000));

        // --- BƯỚC 4: User B Tạo mã VietQR Động yêu cầu thanh toán 50.000 VNĐ ---
        String qrGenJson = String.format(
                "{\"qr_type\":\"DYNAMIC\",\"amount\":50000,\"order_reference\":\"ORDER-E2E-%s\",\"description\":\"Tra tien an trua\"}",
                UUID.randomUUID().toString().substring(0, 8)
        );
        MvcResult qrGenResult = mockMvc.perform(post("/api/v1/qr-codes")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(qrGenJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.payload").hasJsonPath())
                .andReturn();

        String qrPayload = JsonPath.read(qrGenResult.getResponse().getContentAsString(), "$.data.payload");

        // --- BƯỚC 5: User A Quét và Thanh toán mã VietQR đó ---
        String qrPayJson = String.format(
                "{\"qr_payload\":\"%s\",\"pin\":\"123456\"}",
                qrPayload
        );
        mockMvc.perform(post("/api/v1/qr-codes/pay")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(qrPayJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Số dư User A sau 2 lần chuyển (100k + 50k) phải còn đúng 350.000 VNĐ
        Wallet walletA = walletRepository.findById(regA.getWalletId()).orElseThrow();
        assertThat(walletA.getBalance()).isEqualByComparingTo(new BigDecimal("350000"));

        // Số dư User B nhận (100k + 50k) phải là 150.000 VNĐ
        Wallet walletB = walletRepository.findById(regB.getWalletId()).orElseThrow();
        assertThat(walletB.getBalance()).isEqualByComparingTo(new BigDecimal("150000"));

        // --- BƯỚC 6: Admin Đăng nhập & Kiểm tra Đối soát Sổ Cái ---
        AuthResponse adminLogin = authService.login(LoginRequest.builder().phoneNumber("0999999999").password("Admin@123").build());
        String adminToken = adminLogin.getAccessToken();

        mockMvc.perform(get("/api/v1/admin/reconciliation?type=P2P_TRANSFER")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.is_balanced").value(true))
                .andExpect(jsonPath("$.data.net_balance").value(0));

        // --- BƯỚC 7: Admin Khóa Ví User A ---
        mockMvc.perform(put("/api/v1/admin/wallets/" + regA.getWalletId() + "/freeze")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Khoa bao ve tai khoan E2E\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("FROZEN"));

        // --- BƯỚC 8: User A Cố gắng chuyển tiền khi ví đang bị FROZEN -> Bị chặn 403 WALLET_FROZEN ---
        String blockedTransferJson = String.format(
                "{\"dest_phone_number\":\"%s\",\"amount\":20000,\"description\":\"Blocked tx\",\"pin\":\"123456\"}",
                phoneB
        );
        mockMvc.perform(post("/api/v1/transfers")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(blockedTransferJson))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("WALLET_FROZEN"));

        // --- BƯỚC 9: Admin Mở Khóa Ví -> Hoạt động trở lại bình thường ---
        mockMvc.perform(put("/api/v1/admin/wallets/" + regA.getWalletId() + "/unfreeze")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }
}
