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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = com.walletapp.DigitalWalletApiApplication.class)
class DoubleSpendingConcurrencyTest {

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
    @DisplayName("Kịch bản 1: Rút tiền đồng thời chống Double Spending (50 concurrent threads, 100k balance -> Exactly 5 success, balance = 0)")
    void testAntiDoubleSpending_ConcurrentWithdraw() throws Exception {
        long ts = System.currentTimeMillis() % 100000000L;
        String phoneSender = "09" + String.format("%08d", ts);
        String phoneReceiver = "09" + String.format("%08d", ts + 1);

        // 1. Đăng ký ví A và ví B
        RegisterResponse regSender = authService.register(RegisterRequest.builder()
                .phoneNumber(phoneSender)
                .fullName("Sender Concurrency Test")
                .password("Password@123")
                .pin("123456")
                .build());

        authService.register(RegisterRequest.builder()
                .phoneNumber(phoneReceiver)
                .fullName("Receiver Concurrency Test")
                .password("Password@123")
                .pin("123456")
                .build());

        AuthResponse loginSender = authService.login(LoginRequest.builder()
                .phoneNumber(phoneSender)
                .password("Password@123")
                .build());
        String tokenSender = loginSender.getAccessToken();

        // 2. Nạp đúng 100.000 VNĐ cho ví Sender
        mockMvc.perform(post("/api/v1/topup")
                        .header("Authorization", "Bearer " + tokenSender)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":100000,\"payment_method\":\"SIMULATED_BANK\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.new_balance").value(100000));

        // 3. Chuẩn bị 50 threads đồng thời thực hiện chuyển 20.000 VNĐ mỗi request
        int totalRequests = 50;
        int threadPoolSize = 25;
        ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneGate = new CountDownLatch(totalRequests);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger rejectedCount = new AtomicInteger(0);
        AtomicInteger error500Count = new AtomicInteger(0);

        for (int i = 0; i < totalRequests; i++) {
            executor.submit(() -> {
                try {
                    startGate.await(); // Đợi tất cả threads sẵn sàng rồi bắn đồng loạt

                    String reqKey = UUID.randomUUID().toString();
                    String json = String.format(
                            "{\"dest_phone_number\":\"%s\",\"amount\":20000,\"description\":\"Concurrent transfer\",\"pin\":\"123456\"}",
                            phoneReceiver
                    );

                    MvcResult result = mockMvc.perform(post("/api/v1/transfers")
                                    .header("Authorization", "Bearer " + tokenSender)
                                    .header("Idempotency-Key", reqKey)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(json))
                            .andReturn();

                    int status = result.getResponse().getStatus();
                    if (status == 200) {
                        successCount.incrementAndGet();
                    } else if (status == 422 || status == 409 || status == 400) {
                        rejectedCount.incrementAndGet();
                    } else if (status >= 500) {
                        error500Count.incrementAndGet();
                    }
                } catch (Exception e) {
                    rejectedCount.incrementAndGet();
                } finally {
                    doneGate.countDown();
                }
            });
        }

        // Kích nổ toàn bộ threads cùng lúc
        startGate.countDown();
        boolean completed = doneGate.await(60, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();

        // 4. Kiểm tra nghiệm thu Kịch bản 1:
        // - Đúng 5 request thành công (100.000 / 20.000 = 5)
        // - 45 request bị từ chối
        // - 0 lỗi hệ thống 500
        assertThat(successCount.get()).isEqualTo(5);
        assertThat(rejectedCount.get()).isEqualTo(totalRequests - 5);
        assertThat(error500Count.get()).isZero();

        // 5. Kiểm tra số dư ví cuối cùng trong Database đúng bằng 0 VNĐ, không bao giờ âm
        Wallet senderWallet = walletRepository.findById(regSender.getWalletId()).orElseThrow();
        assertThat(senderWallet.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
