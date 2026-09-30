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
public class RegisterResponse {

    @JsonProperty("user_id")
    private UUID userId;

    @JsonProperty("wallet_id")
    private UUID walletId;

    @JsonProperty("phone_number")
    private String phoneNumber;
}
