package com.walletapp.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("refresh_token")
    private String refreshToken;

    @JsonProperty("expires_in")
    private long expiresIn;

    private UserSummary user;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserSummary {
        private UUID id;

        @JsonProperty("full_name")
        private String fullName;

        @JsonProperty("phone_number")
        private String phoneNumber;

        private String role;
    }
}
