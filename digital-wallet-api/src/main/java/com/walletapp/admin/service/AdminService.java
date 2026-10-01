package com.walletapp.admin.service;

import com.walletapp.admin.dto.*;
import com.walletapp.common.dto.PageResponse;
import com.walletapp.common.enums.TransactionStatus;
import com.walletapp.common.enums.TransactionType;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface AdminService {

    DashboardStatsResponse getDashboardStats();

    PageResponse<AdminUserSummaryDto> getUsers(String query, Pageable pageable);

    AdminUserDetailDto getUserDetail(UUID userId);

    FreezeWalletResponse freezeWallet(UUID walletId, String reason);

    FreezeWalletResponse unfreezeWallet(UUID walletId);

    PageResponse<AdminTransactionItemDto> getTransactions(
            TransactionType type,
            TransactionStatus status,
            OffsetDateTime from,
            OffsetDateTime to,
            Pageable pageable
    );

    ReconciliationResponse getReconciliation(OffsetDateTime from, OffsetDateTime to, TransactionType type);

    List<SystemConfigDto> getConfigs();

    SystemConfigDto updateConfig(String configKey, UpdateSystemConfigRequest request, UUID adminId);
}
