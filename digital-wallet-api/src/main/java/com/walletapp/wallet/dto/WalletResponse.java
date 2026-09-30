package com.walletapp.wallet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletResponse {

    @JsonProperty("wallet_id")
    private UUID walletId;

    private BigDecimal balance;

    private String currency;

    private String status;
}
