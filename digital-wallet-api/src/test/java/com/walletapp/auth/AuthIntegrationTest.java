package com.walletapp.auth;

import com.jayway.jsonpath.JsonPath;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = com.walletapp.DigitalWalletApiApplication.class)
class AuthIntegrationTest {

    @Autowired
    private WebApplicationContext context;

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
    @DisplayName("Should successfully register a new user, auto-create a wallet, and then log in")
    void testRegisterAndLoginFlow() throws Exception {
        String uniqueSuffix = String.valueOf(System.currentTimeMillis() % 100000000L);
        while (uniqueSuffix.length() < 8) {
            uniqueSuffix = "0" + uniqueSuffix;
        }
        String testPhone = "09" + uniqueSuffix;

        String registerJson = String.format(
                "{\"phone_number\":\"%s\",\"full_name\":\"Test Runner\",\"password\":\"SecurePass123!\",\"pin\":\"123456\"}",
                testPhone
        );

        // 1. Register
        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phone_number").value(testPhone))
                .andReturn();

        String responseBody = registerResult.getResponse().getContentAsString();
        String userIdStr = JsonPath.read(responseBody, "$.data.user_id");
        String walletIdStr = JsonPath.read(responseBody, "$.data.wallet_id");
        assertThat(userIdStr).isNotNull();
        assertThat(walletIdStr).isNotNull();

        // 2. Verify Wallet was created with 0 balance (BR-SPEC-AUTH03)
        UUID walletId = UUID.fromString(walletIdStr);
        Wallet wallet = walletRepository.findById(walletId).orElse(null);
        assertThat(wallet).isNotNull();
        assertThat(wallet.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(wallet.getCurrency()).isEqualTo("VND");

        // 3. Register again with same phone -> Should get 409 Conflict
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("PHONE_ALREADY_EXISTS"));

        // 4. Login with wrong password -> 401 Unauthorized
        String wrongLoginJson = String.format(
                "{\"phone_number\":\"%s\",\"password\":\"WrongPass123!\"}",
                testPhone
        );
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(wrongLoginJson))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"));

        // 5. Login with correct credentials -> 200 OK
        String correctLoginJson = String.format(
                "{\"phone_number\":\"%s\",\"password\":\"SecurePass123!\"}",
                testPhone
        );
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(correctLoginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.access_token").isNotEmpty())
                .andReturn();

        String loginResponseBody = loginResult.getResponse().getContentAsString();
        String accessToken = JsonPath.read(loginResponseBody, "$.data.access_token");

        // 6. Access protected endpoint /api/v1/wallets/me without token -> 401
        mockMvc.perform(get("/api/v1/wallets/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));

        // 7. Access protected endpoint with Bearer token -> 200 OK
        mockMvc.perform(get("/api/v1/wallets/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.wallet_id").value(walletIdStr))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.currency").value("VND"));
    }
}
