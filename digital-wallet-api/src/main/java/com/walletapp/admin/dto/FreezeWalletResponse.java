package com.walletapp.admin.dto;

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
public class FreezeWalletResponse {

    @JsonProperty("wallet_id")
    private UUID walletId;

    private String status;

    private String reason;
}
