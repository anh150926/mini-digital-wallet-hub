package com.walletapp.transaction.dto;

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
public class TransferResponse {

    @JsonProperty("transaction_id")
    private UUID transactionId;

    private String status;

    private BigDecimal amount;

    private BigDecimal fee;

    @JsonProperty("source_balance_after")
    private BigDecimal sourceBalanceAfter;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;
}
