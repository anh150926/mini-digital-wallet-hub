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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = com.walletapp.DigitalWalletApiApplication.class)
class CrossTransferDeadlockTest {

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
    @DisplayName("Kịch bản 2: Chuyển tiền chéo hai chiều A ↔ B đồng thời chống Deadlock (Lock Ordering & Tổng tiền bảo toàn)")
    void testCrossTransfer_AntiDeadlock() throws Exception {
        long ts = System.currentTimeMillis() % 100000000L;
        String phoneA = "09" + String.format("%08d", ts);
        String phoneB = "09" + String.format("%08d", ts + 1);

        // 1. Đăng ký tài khoản A và B
        RegisterResponse regA = authService.register(RegisterRequest.builder()
                .phoneNumber(phoneA)
                .fullName("User Cross A")
                .password("Pass@12345")
                .pin("123456")
                .build());

        RegisterResponse regB = authService.register(RegisterRequest.builder()
                .phoneNumber(phoneB)
                .fullName("User Cross B")
                .password("Pass@12345")
                .pin("654321")
                .build());

        AuthResponse loginA = authService.login(LoginRequest.builder()
                .phoneNumber(phoneA)
                .password("Pass@12345")
                .build());
        String tokenA = loginA.getAccessToken();

        AuthResponse loginB = authService.login(LoginRequest.builder()
                .phoneNumber(phoneB)
                .password("Pass@12345")
                .build());
        String tokenB = loginB.getAccessToken();

        // 2. Nạp cho A 1.000.000 VNĐ và B 1.000.000 VNĐ (Tổng ban đầu = 2.000.000 VNĐ)
        mockMvc.perform(post("/api/v1/topup")
                .header("Authorization", "Bearer " + tokenA)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":1000000,\"payment_method\":\"SIMULATED_BANK\"}")).andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/topup")
                .header("Authorization", "Bearer " + tokenB)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":1000000,\"payment_method\":\"SIMULATED_BANK\"}")).andExpect(status().isOk());

        // 3. Khởi tạo 15 threads A -> B và 15 threads B -> A chạy đồng thời
        int tasksPerDirection = 15;
        int totalTasks = tasksPerDirection * 2;
        ExecutorService executor = Executors.newFixedThreadPool(16);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneGate = new CountDownLatch(totalTasks);

        AtomicInteger deadlockOr500Count = new AtomicInteger(0);

        for (int i = 0; i < tasksPerDirection; i++) {
            // Luồng A -> B
            executor.submit(() -> {
                try {
                    startGate.await();
                    String json = String.format(
                            "{\"dest_phone_number\":\"%s\",\"amount\":10000,\"description\":\"A to B\",\"pin\":\"123456\"}",
                            phoneB
                    );
                    MvcResult res = mockMvc.perform(post("/api/v1/transfers")
                            .header("Authorization", "Bearer " + tokenA)
                            .header("Idempotency-Key", UUID.randomUUID().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json)).andReturn();
                    if (res.getResponse().getStatus() >= 500) {
                        deadlockOr500Count.incrementAndGet();
                    }
                } catch (Exception e) {
                    deadlockOr500Count.incrementAndGet();
                } finally {
                    doneGate.countDown();
                }
            });

            // Luồng B -> A
            executor.submit(() -> {
                try {
                    startGate.await();
                    String json = String.format(
                            "{\"dest_phone_number\":\"%s\",\"amount\":10000,\"description\":\"B to A\",\"pin\":\"654321\"}",
                            phoneA
                    );
                    MvcResult res = mockMvc.perform(post("/api/v1/transfers")
                            .header("Authorization", "Bearer " + tokenB)
                            .header("Idempotency-Key", UUID.randomUUID().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json)).andReturn();
                    if (res.getResponse().getStatus() >= 500) {
                        deadlockOr500Count.incrementAndGet();
                    }
                } catch (Exception e) {
                    deadlockOr500Count.incrementAndGet();
                } finally {
                    doneGate.countDown();
                }
            });
        }

        startGate.countDown();
        boolean completed = doneGate.await(60, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();

        // 4. Kiểm tra nghiệm thu Kịch bản 2:
        // - Hoàn toàn không có Deadlock PostgreSQL hoặc HTTP 500
        assertThat(deadlockOr500Count.get()).isZero();

        // - Tổng số dư A + B được bảo toàn chính xác = 2.000.000 VNĐ
        Wallet walletA = walletRepository.findById(regA.getWalletId()).orElseThrow();
        Wallet walletB = walletRepository.findById(regB.getWalletId()).orElseThrow();
        BigDecimal totalBalanceAfter = walletA.getBalance().add(walletB.getBalance());
        assertThat(totalBalanceAfter).isEqualByComparingTo(new BigDecimal("2000000"));
    }
}
