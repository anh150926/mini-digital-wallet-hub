package com.walletapp.admin.service.impl;

import com.walletapp.admin.dto.*;
import com.walletapp.admin.repository.SystemConfigRepository;
import com.walletapp.admin.service.AdminService;
import com.walletapp.auth.repository.UserRepository;
import com.walletapp.common.dto.PageResponse;
import com.walletapp.common.enums.*;
import com.walletapp.common.exception.BusinessException;
import com.walletapp.entity.SystemConfig;
import com.walletapp.entity.User;
import com.walletapp.transaction.entity.Transaction;
import com.walletapp.transaction.repository.LedgerEntryRepository;
import com.walletapp.transaction.repository.TransactionRepository;
import com.walletapp.wallet.entity.Wallet;
import com.walletapp.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final SystemConfigRepository systemConfigRepository;

    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Override
    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats() {
        long totalUsers = userRepository.count();
        long totalActiveWallets = walletRepository.countByStatus(WalletStatus.ACTIVE);

        LocalDate today = LocalDate.now(VIETNAM_ZONE);
        OffsetDateTime startOfToday = today.atStartOfDay(VIETNAM_ZONE).toOffsetDateTime();
        OffsetDateTime endOfToday = startOfToday.plusDays(1);

        long todayTxCount = transactionRepository.countByCreatedAtBetweenAndStatus(
                startOfToday, endOfToday, TransactionStatus.SUCCESS
        );
        BigDecimal todayTxVolume = transactionRepository.sumVolumeBetween(startOfToday, endOfToday);

        List<DailyStatDto> dailyStats = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            OffsetDateTime dStart = d.atStartOfDay(VIETNAM_ZONE).toOffsetDateTime();
            OffsetDateTime dEnd = dStart.plusDays(1);

            long count = transactionRepository.countByCreatedAtBetweenAndStatus(
                    dStart, dEnd, TransactionStatus.SUCCESS
            );
            BigDecimal vol = transactionRepository.sumVolumeBetween(dStart, dEnd);

            dailyStats.add(DailyStatDto.builder()
                    .date(d.toString())
                    .count(count)
                    .volume(vol)
                    .build());
        }

        return DashboardStatsResponse.builder()
                .totalUsers(totalUsers)
                .totalActiveWallets(totalActiveWallets)
                .todayTransactionCount(todayTxCount)
                .todayTransactionVolume(todayTxVolume)
                .dailyStats(dailyStats)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminUserSummaryDto> getUsers(String query, Pageable pageable) {
        Page<User> userPage = userRepository.searchUsers(query, pageable);
        List<UUID> userIds = userPage.getContent().stream().map(User::getId).toList();

        Map<UUID, Wallet> walletMap = userIds.isEmpty() ? Collections.emptyMap() :
                walletRepository.findByUserIdIn(userIds).stream()
                        .collect(Collectors.toMap(Wallet::getUserId, Function.identity(), (a, b) -> a));

        List<AdminUserSummaryDto> content = userPage.getContent().stream().map(user -> {
            Wallet wallet = walletMap.get(user.getId());
            return AdminUserSummaryDto.builder()
                    .userId(user.getId())
                    .phoneNumber(user.getPhoneNumber())
                    .fullName(user.getFullName())
                    .role(user.getRole().name())
                    .userStatus(user.getStatus().name())
                    .walletId(wallet != null ? wallet.getId() : null)
                    .balance(wallet != null ? wallet.getBalance() : BigDecimal.ZERO)
                    .walletStatus(wallet != null ? wallet.getStatus().name() : "NONE")
                    .createdAt(user.getCreatedAt())
                    .build();
        }).toList();

        return PageResponse.<AdminUserSummaryDto>builder()
                .content(content)
                .page(userPage.getNumber())
                .size(userPage.getSize())
                .totalElements(userPage.getTotalElements())
                .totalPages(userPage.getTotalPages())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AdminUserDetailDto getUserDetail(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WALLET_NOT_FOUND));

        List<Transaction> txList = transactionRepository.findTop10BySourceWalletIdOrDestWalletIdOrderByCreatedAtDesc(
                wallet.getId(), wallet.getId()
        );

        List<AdminTransactionItemDto> recentTxDtos = txList.stream().map(tx -> AdminTransactionItemDto.builder()
                .id(tx.getId())
                .idempotencyKey(tx.getIdempotencyKey())
                .sourceWalletId(tx.getSourceWalletId())
                .destWalletId(tx.getDestWalletId())
                .amount(tx.getAmount())
                .fee(tx.getFee())
                .type(tx.getType().name())
                .status(tx.getStatus().name())
                .description(tx.getDescription())
                .createdAt(tx.getCreatedAt())
                .build()
        ).toList();

        return AdminUserDetailDto.builder()
                .userId(user.getId())
                .phoneNumber(user.getPhoneNumber())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .userStatus(user.getStatus().name())
                .walletId(wallet.getId())
                .balance(wallet.getBalance())
                .walletStatus(wallet.getStatus().name())
                .createdAt(user.getCreatedAt())
                .recentTransactions(recentTxDtos)
                .build();
    }

    @Override
    @Transactional
    public FreezeWalletResponse freezeWallet(UUID walletId, String reason) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WALLET_NOT_FOUND));

        wallet.setStatus(WalletStatus.FROZEN);
        walletRepository.save(wallet);

        log.warn("Admin đã đóng băng ví {}. Lý do: {}", walletId, reason);

        return FreezeWalletResponse.builder()
                .walletId(walletId)
                .status(WalletStatus.FROZEN.name())
                .reason(reason)
                .build();
    }

    @Override
    @Transactional
    public FreezeWalletResponse unfreezeWallet(UUID walletId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WALLET_NOT_FOUND));

        wallet.setStatus(WalletStatus.ACTIVE);
        walletRepository.save(wallet);

        log.info("Admin đã mở khóa ví {}", walletId);

        return FreezeWalletResponse.builder()
                .walletId(walletId)
                .status(WalletStatus.ACTIVE.name())
                .reason("Mở khóa ví thành công")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminTransactionItemDto> getTransactions(
            TransactionType type,
            TransactionStatus status,
            OffsetDateTime from,
            OffsetDateTime to,
            Pageable pageable
    ) {
        Page<Transaction> page = transactionRepository.findAllWithFilters(type, status, from, to, pageable);

        List<AdminTransactionItemDto> dtos = page.getContent().stream().map(tx -> AdminTransactionItemDto.builder()
                .id(tx.getId())
                .idempotencyKey(tx.getIdempotencyKey())
                .sourceWalletId(tx.getSourceWalletId())
                .destWalletId(tx.getDestWalletId())
                .amount(tx.getAmount())
                .fee(tx.getFee())
                .type(tx.getType().name())
                .status(tx.getStatus().name())
                .description(tx.getDescription())
                .createdAt(tx.getCreatedAt())
                .build()
        ).toList();

        return PageResponse.<AdminTransactionItemDto>builder()
                .content(dtos)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ReconciliationResponse getReconciliation(OffsetDateTime from, OffsetDateTime to, TransactionType type) {
        OffsetDateTime fromDate = from != null ? from : OffsetDateTime.now().minusDays(30);
        OffsetDateTime toDate = to != null ? to : OffsetDateTime.now();

        BigDecimal totalDebit;
        BigDecimal totalCredit;
        long txCount;
        long debitCount;
        long creditCount;

        if (type != null) {
            totalDebit = ledgerEntryRepository.sumByEntryTypeAndTypeAndDateRange(EntryType.DEBIT, type, fromDate, toDate);
            totalCredit = ledgerEntryRepository.sumByEntryTypeAndTypeAndDateRange(EntryType.CREDIT, type, fromDate, toDate);
            txCount = ledgerEntryRepository.countDistinctTransactionsByTypeAndDateRange(type, fromDate, toDate);
            debitCount = ledgerEntryRepository.countByEntryTypeAndTypeAndDateRange(EntryType.DEBIT, type, fromDate, toDate);
            creditCount = ledgerEntryRepository.countByEntryTypeAndTypeAndDateRange(EntryType.CREDIT, type, fromDate, toDate);
        } else {
            totalDebit = ledgerEntryRepository.sumByEntryTypeAndDateRange(EntryType.DEBIT, fromDate, toDate);
            totalCredit = ledgerEntryRepository.sumByEntryTypeAndDateRange(EntryType.CREDIT, fromDate, toDate);
            txCount = ledgerEntryRepository.countDistinctTransactionsByDateRange(fromDate, toDate);
            debitCount = ledgerEntryRepository.countByEntryTypeAndDateRange(EntryType.DEBIT, fromDate, toDate);
            creditCount = ledgerEntryRepository.countByEntryTypeAndDateRange(EntryType.CREDIT, fromDate, toDate);
        }

        BigDecimal net = totalCredit.subtract(totalDebit);
        boolean isBalanced = (net.compareTo(BigDecimal.ZERO) == 0);

        return ReconciliationResponse.builder()
                .totalDebit(totalDebit)
                .totalCredit(totalCredit)
                .netBalance(net)
                .balanced(isBalanced)
                .transactionCount(txCount)
                .debitEntryCount(debitCount)
                .creditEntryCount(creditCount)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SystemConfigDto> getConfigs() {
        return systemConfigRepository.findAll().stream()
                .sorted(Comparator.comparing(SystemConfig::getConfigKey))
                .map(cfg -> SystemConfigDto.builder()
                        .id(cfg.getId())
                        .configKey(cfg.getConfigKey())
                        .configValue(cfg.getConfigValue())
                        .description(cfg.getDescription())
                        .updatedAt(cfg.getUpdatedAt())
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public SystemConfigDto updateConfig(String configKey, UpdateSystemConfigRequest request, UUID adminId) {
        SystemConfig config = systemConfigRepository.findByConfigKey(configKey)
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_FAILED, "Cấu hình " + configKey + " không tồn tại"));

        log.info("Admin {} cập nhật cấu hình {}: '{}' -> '{}'. Lý do: {}",
                adminId, configKey, config.getConfigValue(), request.getValue(), request.getReason());

        config.setConfigValue(request.getValue());
        config.setUpdatedBy(adminId);
        SystemConfig saved = systemConfigRepository.save(config);

        return SystemConfigDto.builder()
                .id(saved.getId())
                .configKey(saved.getConfigKey())
                .configValue(saved.getConfigValue())
                .description(saved.getDescription())
                .updatedAt(saved.getUpdatedAt())
                .build();
    }
}
