package com.walletapp.qrcode.service;

import com.walletapp.admin.repository.SystemConfigRepository;
import com.walletapp.common.enums.ErrorCode;
import com.walletapp.common.enums.QrType;
import com.walletapp.common.exception.BusinessException;
import com.walletapp.common.util.VietQrUtils;
import com.walletapp.qrcode.dto.CreateQrRequest;
import com.walletapp.qrcode.dto.QrCodeResponse;
import com.walletapp.qrcode.entity.QrCode;
import com.walletapp.qrcode.repository.QrCodeRepository;
import com.walletapp.wallet.entity.Wallet;
import com.walletapp.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class QrCodeGeneratorService {

    private final QrCodeRepository qrCodeRepository;
    private final WalletRepository walletRepository;
    private final SystemConfigRepository systemConfigRepository;

    @Transactional
    public QrCodeResponse generateQrCode(UUID userId, CreateQrRequest request) {
        log.info("Generating QR code for userId={}, amount={}, type={}",
                userId, request.getAmount(), request.getQrType());

        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WALLET_NOT_FOUND));

        boolean isDynamic = !"STATIC".equalsIgnoreCase(request.getQrType());
        BigDecimal amount = request.getAmount();

        if (isDynamic && (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Mã QR động bắt buộc phải có số tiền lớn hơn 0");
        }

        // Generate EMVCo TLV string with Tag 38 & CRC16 checksum
        String payload = VietQrUtils.generateVietQrPayload(
                wallet.getId().toString(),
                VietQrUtils.DEFAULT_BIN,
                amount,
                request.getDescription(),
                isDynamic
        );

        OffsetDateTime expiryTime = null;
        if (isDynamic) {
            long expiryMinutes = getDynamicExpiryMinutes();
            expiryTime = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(expiryMinutes);
        }

        QrCode qrCode = QrCode.builder()
                .walletId(wallet.getId())
                .amount(amount)
                .orderReference(request.getOrderReference())
                .qrType(isDynamic ? QrType.DYNAMIC : QrType.STATIC)
                .payload(payload)
                .expiryTime(expiryTime)
                .isUsed(false)
                .build();

        qrCode = qrCodeRepository.save(qrCode);

        log.info("QR code generated successfully. id={}, type={}, expiry={}",
                qrCode.getId(), qrCode.getQrType(), qrCode.getExpiryTime());

        return QrCodeResponse.builder()
                .qrId(qrCode.getId())
                .payload(qrCode.getPayload())
                .qrType(qrCode.getQrType().name())
                .amount(qrCode.getAmount())
                .expiryTime(qrCode.getExpiryTime())
                .build();
    }

    private long getDynamicExpiryMinutes() {
        try {
            return systemConfigRepository.findByConfigKey("QR_DYNAMIC_EXPIRY_MINUTES")
                    .map(cfg -> Long.parseLong(cfg.getConfigValue()))
                    .orElse(15L);
        } catch (Exception e) {
            return 15L;
        }
    }
}
