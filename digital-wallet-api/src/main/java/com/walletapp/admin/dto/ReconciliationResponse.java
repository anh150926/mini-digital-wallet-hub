package com.walletapp.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReconciliationResponse {

    @JsonProperty("total_debit")
    private BigDecimal totalDebit;

    @JsonProperty("total_credit")
    private BigDecimal totalCredit;

    @JsonProperty("net_balance")
    private BigDecimal netBalance;

    @JsonProperty("is_balanced")
    private boolean balanced;

    @JsonProperty("transaction_count")
    private long transactionCount;

    @JsonProperty("debit_entry_count")
    private long debitEntryCount;

    @JsonProperty("credit_entry_count")
    private long creditEntryCount;
}
