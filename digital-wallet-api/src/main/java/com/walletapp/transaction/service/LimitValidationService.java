package com.walletapp.transaction.service;

import com.walletapp.admin.repository.SystemConfigRepository;
import com.walletapp.common.enums.ErrorCode;
import com.walletapp.common.enums.WalletStatus;
import com.walletapp.common.exception.BusinessException;
import com.walletapp.transaction.repository.TransactionRepository;
import com.walletapp.wallet.entity.Wallet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Slf4j
@Service
@RequiredArgsConstructor
public class LimitValidationService {

    private final SystemConfigRepository systemConfigRepository;
    private final TransactionRepository transactionRepository;

    public void validateTransferLimits(Wallet sourceWallet, Wallet destWallet, BigDecimal amount) {
        // 1. Check self transfer (BR-SPEC-TX02)
        if (sourceWallet.getId().equals(destWallet.getId())) {
            throw new BusinessException(ErrorCode.SELF_TRANSFER_NOT_ALLOWED);
        }

        // 2. Check wallet statuses (BR-SPEC-TX04)
        if (sourceWallet.getStatus() != WalletStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.WALLET_FROZEN, "Ví người gửi đang bị tạm khóa");
        }
        if (destWallet.getStatus() != WalletStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.WALLET_FROZEN, "Ví người nhận đang bị tạm khóa");
        }

        // 3. Minimum amount
        BigDecimal minAmount = getConfigBigDecimal("TX_MIN_AMOUNT", new BigDecimal("10000"));
        if (amount.compareTo(minAmount) < 0) {
            throw new BusinessException(ErrorCode.BELOW_MINIMUM_AMOUNT,
                    "Số tiền giao dịch tối thiểu là " + minAmount.toPlainString() + " VNĐ");
        }

        // 4. Maximum per transaction
        BigDecimal maxPerTx = getConfigBigDecimal("TX_MAX_PER_TRANSACTION", new BigDecimal("5000000"));
        if (amount.compareTo(maxPerTx) > 0) {
            throw new BusinessException(ErrorCode.EXCEEDS_PER_TRANSACTION_LIMIT,
                    "Số tiền vượt hạn mức tối đa mỗi lần giao dịch (" + maxPerTx.toPlainString() + " VNĐ)");
        }

        // 5. Maximum daily limit
        BigDecimal maxDaily = getConfigBigDecimal("TX_MAX_DAILY", new BigDecimal("20000000"));
        OffsetDateTime startOfDay = OffsetDateTime.now(ZoneOffset.UTC).toLocalDate().atStartOfDay().atOffset(ZoneOffset.UTC);
        BigDecimal currentDailyTotal = transactionRepository.sumDailyOutgoingAmount(sourceWallet.getId(), startOfDay);
        if (currentDailyTotal.add(amount).compareTo(maxDaily) > 0) {
            throw new BusinessException(ErrorCode.EXCEEDS_DAILY_LIMIT,
                    "Giao dịch vượt quá hạn mức tối đa trong ngày (" + maxDaily.toPlainString() + " VNĐ)");
        }

        // 6. Dest wallet max balance limit
        BigDecimal maxWalletBalance = getConfigBigDecimal("WALLET_MAX_BALANCE", new BigDecimal("100000000"));
        if (destWallet.getBalance().add(amount).compareTo(maxWalletBalance) > 0) {
            throw new BusinessException(ErrorCode.DEST_WALLET_EXCEEDS_MAX_BALANCE,
                    "Số dư ví người nhận sẽ vượt quá giới hạn tối đa cho phép (" + maxWalletBalance.toPlainString() + " VNĐ)");
        }
    }

    private BigDecimal getConfigBigDecimal(String key, BigDecimal defaultValue) {
        try {
            return systemConfigRepository.findByConfigKey(key)
                    .map(cfg -> new BigDecimal(cfg.getConfigValue()))
                    .orElse(defaultValue);
        } catch (Exception e) {
            log.warn("Failed to parse system config for key: {}, using default: {}", key, defaultValue);
            return defaultValue;
        }
    }
}
