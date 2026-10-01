package com.walletapp.admin;

import com.jayway.jsonpath.JsonPath;
import com.walletapp.auth.dto.AuthResponse;
import com.walletapp.auth.dto.LoginRequest;
import com.walletapp.auth.dto.RegisterRequest;
import com.walletapp.auth.dto.RegisterResponse;
import com.walletapp.auth.repository.UserRepository;
import com.walletapp.auth.service.AuthService;
import com.walletapp.common.enums.Role;
import com.walletapp.common.enums.UserStatus;
import com.walletapp.common.enums.WalletStatus;
import com.walletapp.entity.User;
import com.walletapp.wallet.entity.Wallet;
import com.walletapp.wallet.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = com.walletapp.DigitalWalletApiApplication.class)
class AdminIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;

    private String adminToken;
    private String userToken;
    private UUID testUserWalletId;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // Đảm bảo có tài khoản Admin cho test
        String adminPhone = "0999999999";
        if (!userRepository.existsByPhoneNumber(adminPhone)) {
            User admin = User.builder()
                    .phoneNumber(adminPhone)
                    .fullName("Admin Test")
                    .passwordHash(passwordEncoder.encode("Admin@123"))
                    .pinHash(passwordEncoder.encode("123456"))
                    .role(Role.ADMIN)
                    .status(UserStatus.ACTIVE)
                    .build();
            User saved = userRepository.save(admin);
            Wallet w = Wallet.builder()
                    .userId(saved.getId())
                    .balance(new BigDecimal("10000000"))
                    .currency("VND")
                    .status(WalletStatus.ACTIVE)
                    .build();
            walletRepository.save(w);
        }

        AuthResponse adminLogin = authService.login(LoginRequest.builder()
                .phoneNumber(adminPhone)
                .password("Admin@123")
                .build());
        this.adminToken = adminLogin.getAccessToken();

        // Tạo user thường ngẫu nhiên để test phân quyền và đóng băng ví
        String suffix = String.valueOf(System.currentTimeMillis() % 10000000);
        String userPhone = "08" + String.format("%08d", Long.parseLong(suffix));
        RegisterResponse reg = authService.register(RegisterRequest.builder()
                .phoneNumber(userPhone)
                .fullName("Regular User")
                .password("User@1234")
                .pin("654321")
                .build());
        this.testUserWalletId = reg.getWalletId();

        AuthResponse userLogin = authService.login(LoginRequest.builder()
                .phoneNumber(userPhone)
                .password("User@1234")
                .build());
        this.userToken = userLogin.getAccessToken();
    }

    @Test
    @DisplayName("ADM-01: Admin lấy dữ liệu Dashboard thống kê thành công")
    void testGetDashboardStats_Success() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.total_users").isNumber())
                .andExpect(jsonPath("$.data.total_active_wallets").isNumber())
                .andExpect(jsonPath("$.data.daily_stats").isArray());
    }

    @Test
    @DisplayName("Security: Người dùng thường (ROLE_USER) gọi API Admin bị từ chối 403 Forbidden")
    void testRegularUser_AccessDenied() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("ADM-02: Admin tìm kiếm danh sách users phân trang")
    void testGetUsers_Success() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users?page=0&size=10")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @DisplayName("ADM-02: Admin đóng băng và mở khóa ví người dùng (Freeze / Unfreeze)")
    void testFreezeAndUnfreezeWallet_Success() throws Exception {
        // 1. Freeze
        String freezeBody = "{\"reason\":\"Nghi vấn gian lận test\"}";
        mockMvc.perform(put("/api/v1/admin/wallets/" + testUserWalletId + "/freeze")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(freezeBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("FROZEN"));

        Wallet frozenWallet = walletRepository.findById(testUserWalletId).orElseThrow();
        assertThat(frozenWallet.getStatus()).isEqualTo(WalletStatus.FROZEN);

        // 2. Unfreeze
        mockMvc.perform(put("/api/v1/admin/wallets/" + testUserWalletId + "/unfreeze")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        Wallet activeWallet = walletRepository.findById(testUserWalletId).orElseThrow();
        assertThat(activeWallet.getStatus()).isEqualTo(WalletStatus.ACTIVE);
    }

    @Test
    @DisplayName("ADM-03: Admin xem báo cáo đối soát sổ cái kế toán kép (Reconciliation)")
    void testGetReconciliation_Success() throws Exception {
        mockMvc.perform(get("/api/v1/admin/reconciliation")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.total_debit").hasJsonPath())
                .andExpect(jsonPath("$.data.total_credit").hasJsonPath())
                .andExpect(jsonPath("$.data.is_balanced").isBoolean());
    }

    @Test
    @DisplayName("ADM-04: Admin xem danh sách cấu hình hệ thống")
    void testGetConfigs_Success() throws Exception {
        mockMvc.perform(get("/api/v1/admin/configs")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }
}
