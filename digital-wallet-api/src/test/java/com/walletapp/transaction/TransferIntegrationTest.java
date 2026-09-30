package com.walletapp.transaction;

import com.jayway.jsonpath.JsonPath;
import com.walletapp.auth.service.AuthService;
import com.walletapp.auth.dto.RegisterRequest;
import com.walletapp.auth.dto.RegisterResponse;
import com.walletapp.auth.dto.LoginRequest;
import com.walletapp.auth.dto.AuthResponse;
import com.walletapp.wallet.entity.Wallet;
import com.walletapp.wallet.repository.WalletRepository;
import com.walletapp.transaction.repository.LedgerEntryRepository;
import com.walletapp.transaction.repository.TransactionRepository;
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

@SpringBootTest
class TransferIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private AuthService authService;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("Comprehensive P2P Transfer: Lock Ordering, Double-Entry Ledger, Idempotency & Limits")
    void testTransferFlow() throws Exception {
        long ts = System.currentTimeMillis() % 100000000L;
        String phoneA = "09" + String.format("%08d", ts);
        String phoneB = "09" + String.format("%08d", ts + 1);

        // 1. Register User A and User B
        RegisterResponse regA = authService.register(RegisterRequest.builder()
                .phoneNumber(phoneA)
                .fullName("User Sender A")
                .password("SecurePass123!")
                .pin("123456")
                .build());

        RegisterResponse regB = authService.register(RegisterRequest.builder()
                .phoneNumber(phoneB)
                .fullName("User Receiver B")
                .password("SecurePass123!")
                .pin("654321")
                .build());

        AuthResponse loginA = authService.login(LoginRequest.builder()
                .phoneNumber(phoneA)
                .password("SecurePass123!")
                .build());
        String tokenA = loginA.getAccessToken();

        // 2. Top-up 2,000,000 VND to User A
        String topupKey = UUID.randomUUID().toString();
        String topupJson = "{\"amount\":2000000,\"payment_method\":\"SIMULATED_BANK\"}";

        mockMvc.perform(post("/api/v1/topup")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("Idempotency-Key", topupKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(topupJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.new_balance").value(2000000));

        // 3. User A transfers 500,000 VND to User B
        String transferKey = UUID.randomUUID().toString();
        String transferJson = String.format(
                "{\"dest_phone_number\":\"%s\",\"amount\":500000,\"description\":\"Test payment\",\"pin\":\"123456\"}",
                phoneB
        );

        MvcResult transferResult = mockMvc.perform(post("/api/v1/transfers")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("Idempotency-Key", transferKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.amount").value(500000))
                .andExpect(jsonPath("$.data.fee").value(0))
                .andExpect(jsonPath("$.data.source_balance_after").value(1500000))
                .andReturn();

        String txIdStr = JsonPath.read(transferResult.getResponse().getContentAsString(), "$.data.transaction_id");
        UUID txId = UUID.fromString(txIdStr);

        // 4. Verify balances in DB
        Wallet walletA = walletRepository.findById(regA.getWalletId()).orElseThrow();
        Wallet walletB = walletRepository.findById(regB.getWalletId()).orElseThrow();
        assertThat(walletA.getBalance()).isEqualByComparingTo(new BigDecimal("1500000"));
        assertThat(walletB.getBalance()).isEqualByComparingTo(new BigDecimal("500000"));

        // 5. Verify Double-Entry Ledger (BR-GEN-05: Sum Debit == Sum Credit)
        var ledgerEntries = ledgerEntryRepository.findByTransactionId(txId);
        assertThat(ledgerEntries).hasSize(2);
        BigDecimal sumDebit = BigDecimal.ZERO;
        BigDecimal sumCredit = BigDecimal.ZERO;
        for (var entry : ledgerEntries) {
            if ("DEBIT".equals(entry.getEntryType().name())) {
                sumDebit = sumDebit.add(entry.getAmount());
            } else if ("CREDIT".equals(entry.getEntryType().name())) {
                sumCredit = sumCredit.add(entry.getAmount());
            }
        }
        assertThat(sumDebit).isEqualByComparingTo(sumCredit);
        assertThat(sumDebit).isEqualByComparingTo(new BigDecimal("500000"));

        // 6. Test Idempotency: Send the exact same request with same Key -> Returns cached 200, no balance change!
        mockMvc.perform(post("/api/v1/transfers")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("Idempotency-Key", transferKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.transaction_id").value(txIdStr))
                .andExpect(jsonPath("$.data.source_balance_after").value(1500000));

        // Re-check wallet balances didn't change
        walletA = walletRepository.findById(regA.getWalletId()).orElseThrow();
        assertThat(walletA.getBalance()).isEqualByComparingTo(new BigDecimal("1500000"));

        // 7. Test Idempotency Tampering: Same Key, different payload -> 422 IDEMPOTENCY_PAYLOAD_MISMATCH
        String tamperedJson = String.format(
                "{\"dest_phone_number\":\"%s\",\"amount\":999000,\"description\":\"Hacked\",\"pin\":\"123456\"}",
                phoneB
        );
        mockMvc.perform(post("/api/v1/transfers")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("Idempotency-Key", transferKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tamperedJson))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("IDEMPOTENCY_PAYLOAD_MISMATCH"));

        // 8. Test Self Transfer: A transfers to A -> 422 SELF_TRANSFER_NOT_ALLOWED
        String selfTransferKey = UUID.randomUUID().toString();
        String selfTransferJson = String.format(
                "{\"dest_phone_number\":\"%s\",\"amount\":50000,\"description\":\"Self\",\"pin\":\"123456\"}",
                phoneA
        );
        mockMvc.perform(post("/api/v1/transfers")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("Idempotency-Key", selfTransferKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(selfTransferJson))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("SELF_TRANSFER_NOT_ALLOWED"));

        // 9. Test Wrong PIN -> 401 INVALID_PIN
        String wrongPinKey = UUID.randomUUID().toString();
        String wrongPinJson = String.format(
                "{\"dest_phone_number\":\"%s\",\"amount\":50000,\"description\":\"Wrong PIN\",\"pin\":\"000000\"}",
                phoneB
        );
        mockMvc.perform(post("/api/v1/transfers")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("Idempotency-Key", wrongPinKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(wrongPinJson))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_PIN"));
    }
}
