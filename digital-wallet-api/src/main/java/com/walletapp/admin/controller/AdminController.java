package com.walletapp.admin.controller;

import com.walletapp.admin.dto.*;
import com.walletapp.admin.service.AdminService;
import com.walletapp.common.dto.ApiResponse;
import com.walletapp.common.dto.PageResponse;
import com.walletapp.common.enums.TransactionStatus;
import com.walletapp.common.enums.TransactionType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'OWNER')")
@Tag(name = "Admin Portal", description = "Quản lý hệ thống, thống kê, kiểm soát ví và đối soát kế toán kép")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/dashboard")
    @Operation(summary = "ADM-01: Lấy thống kê tổng quan Dashboard")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getDashboardStats() {
        DashboardStatsResponse stats = adminService.getDashboardStats();
        return ResponseEntity.ok(ApiResponse.ok(stats));
    }

    @GetMapping("/users")
    @Operation(summary = "ADM-02: Lấy danh sách người dùng có tìm kiếm và phân trang")
    public ResponseEntity<ApiResponse<PageResponse<AdminUserSummaryDto>>> getUsers(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("createdAt").descending());
        PageResponse<AdminUserSummaryDto> response = adminService.getUsers(search, pageable);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/users/{userId}")
    @Operation(summary = "ADM-02: Lấy thông tin chi tiết người dùng và ví")
    public ResponseEntity<ApiResponse<AdminUserDetailDto>> getUserDetail(
            @PathVariable UUID userId
    ) {
        AdminUserDetailDto detail = adminService.getUserDetail(userId);
        return ResponseEntity.ok(ApiResponse.ok(detail));
    }

    @PutMapping("/wallets/{walletId}/freeze")
    @Operation(summary = "ADM-02: Đóng băng/khóa ví người dùng khẩn cấp")
    public ResponseEntity<ApiResponse<FreezeWalletResponse>> freezeWallet(
            @PathVariable UUID walletId,
            @Valid @RequestBody FreezeWalletRequest request
    ) {
        FreezeWalletResponse response = adminService.freezeWallet(walletId, request.getReason());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping("/wallets/{walletId}/unfreeze")
    @Operation(summary = "ADM-02: Mở khóa ví người dùng")
    public ResponseEntity<ApiResponse<FreezeWalletResponse>> unfreezeWallet(
            @PathVariable UUID walletId
    ) {
        FreezeWalletResponse response = adminService.unfreezeWallet(walletId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/transactions")
    @Operation(summary = "ADM-03: Lấy danh sách lịch sử giao dịch toàn hệ thống có lọc")
    public ResponseEntity<ApiResponse<PageResponse<AdminTransactionItemDto>>> getTransactions(
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        PageResponse<AdminTransactionItemDto> response = adminService.getTransactions(type, status, from, to, pageable);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/reconciliation")
    @Operation(summary = "ADM-03: Báo cáo đối soát sổ cái kế toán kép (Dual-entry Ledger)")
    public ResponseEntity<ApiResponse<ReconciliationResponse>> getReconciliation(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(required = false) TransactionType type
    ) {
        ReconciliationResponse response = adminService.getReconciliation(from, to, type);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/configs")
    @Operation(summary = "ADM-04: Lấy danh sách cấu hình hệ thống (hạn mức, biểu phí)")
    public ResponseEntity<ApiResponse<List<SystemConfigDto>>> getConfigs() {
        List<SystemConfigDto> configs = adminService.getConfigs();
        return ResponseEntity.ok(ApiResponse.ok(configs));
    }

    @PutMapping("/configs/{configKey}")
    @Operation(summary = "ADM-04: Cập nhật cấu hình hệ thống")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<SystemConfigDto>> updateConfig(
            @PathVariable String configKey,
            @Valid @RequestBody UpdateSystemConfigRequest request,
            @AuthenticationPrincipal UUID adminId
    ) {
        SystemConfigDto updated = adminService.updateConfig(configKey, request, adminId);
        return ResponseEntity.ok(ApiResponse.ok(updated));
    }
}
