package com.walletapp.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {

    @JsonProperty("total_users")
    private long totalUsers;

    @JsonProperty("total_active_wallets")
    private long totalActiveWallets;

    @JsonProperty("today_transaction_count")
    private long todayTransactionCount;

    @JsonProperty("today_transaction_volume")
    private BigDecimal todayTransactionVolume;

    @JsonProperty("daily_stats")
    private List<DailyStatDto> dailyStats;
}
