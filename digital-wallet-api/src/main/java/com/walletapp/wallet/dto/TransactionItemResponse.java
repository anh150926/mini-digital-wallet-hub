package com.walletapp.wallet.dto;

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
public class TransactionItemResponse {

    private UUID id;

    private String type;

    private String status;

    private BigDecimal amount;

    private BigDecimal fee;

    private String direction;

    @JsonProperty("counterparty_name")
    private String counterpartyName;

    @JsonProperty("counterparty_phone")
    private String counterpartyPhone;

    private String description;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;
}
