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
public class AdminTransactionItemDto {

    private UUID id;

    @JsonProperty("idempotency_key")
    private String idempotencyKey;

    @JsonProperty("source_wallet_id")
    private UUID sourceWalletId;

    @JsonProperty("dest_wallet_id")
    private UUID destWalletId;

    @JsonProperty("source_phone")
    private String sourcePhone;

    @JsonProperty("source_name")
    private String sourceName;

    @JsonProperty("dest_phone")
    private String destPhone;

    @JsonProperty("dest_name")
    private String destName;

    private BigDecimal amount;
    private BigDecimal fee;
    private String type;
    private String status;
    private String description;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;
}
