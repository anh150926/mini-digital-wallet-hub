package com.walletapp.transaction.dto;

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
public class TopupResponse {

    @JsonProperty("transaction_id")
    private UUID transactionId;

    private String status;

    @JsonProperty("new_balance")
    private BigDecimal newBalance;
}
