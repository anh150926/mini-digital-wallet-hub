package com.walletapp.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserSummaryDto {

    @JsonProperty("user_id")
    private UUID userId;

    @JsonProperty("phone_number")
    private String phoneNumber;

    @JsonProperty("full_name")
    private String fullName;

    private String role;

    @JsonProperty("user_status")
    private String userStatus;

    @JsonProperty("wallet_id")
    private UUID walletId;

    private BigDecimal balance;

    @JsonProperty("wallet_status")
    private String walletStatus;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;
}
