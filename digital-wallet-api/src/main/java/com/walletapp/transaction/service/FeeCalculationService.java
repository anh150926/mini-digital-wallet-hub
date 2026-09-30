package com.walletapp.transaction.service;

import com.walletapp.admin.repository.SystemConfigRepository;
import com.walletapp.common.enums.TransactionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeeCalculationService {

    private final SystemConfigRepository systemConfigRepository;

    public BigDecimal calculateFee(TransactionType type, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        return switch (type) {
            case P2P_TRANSFER, TOP_UP, QR_PAYMENT -> {
                // Free according to BR-SPEC-TX03
                yield BigDecimal.ZERO;
            }
            case WITHDRAW -> {
                // Fixed fee + percentage fee
                BigDecimal fixedFee = getConfigBigDecimal("FEE_WITHDRAW_FIXED", new BigDecimal("1100"));
                BigDecimal percentFee = getConfigBigDecimal("FEE_WITHDRAW_PERCENT", new BigDecimal("0.001"));

                BigDecimal variableFee = amount.multiply(percentFee).setScale(0, RoundingMode.HALF_UP);
                yield fixedFee.add(variableFee);
            }
        };
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
