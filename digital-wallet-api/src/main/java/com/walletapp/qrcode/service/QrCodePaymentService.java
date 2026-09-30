package com.walletapp.qrcode.service;

import com.walletapp.auth.repository.UserRepository;
import com.walletapp.common.enums.ErrorCode;
import com.walletapp.common.enums.TransactionType;
import com.walletapp.common.exception.BusinessException;
import com.walletapp.common.util.VietQrUtils;
import com.walletapp.entity.User;
import com.walletapp.qrcode.dto.QrPaymentRequest;
import com.walletapp.qrcode.dto.QrPaymentResponse;
import com.walletapp.qrcode.entity.QrCode;
import com.walletapp.qrcode.repository.QrCodeRepository;
import com.walletapp.security.service.PinService;
import com.walletapp.transaction.service.FeeCalculationService;
import com.walletapp.transaction.service.LimitValidationService;
import com.walletapp.transaction.service.TransferExecutor;
import com.walletapp.wallet.entity.Wallet;
import com.walletapp.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class QrCodePaymentService {

    private final QrCodeRepository qrCodeRepository;
    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final PinService pinService;
    private final LimitValidationService limitValidationService;
    private final FeeCalculationService feeCalculationService;
    private final TransferExecutor transferExecutor;
    private final RedissonClient redissonClient;

    public QrPaymentResponse processPayment(UUID userId, QrPaymentRequest request, String idempotencyKey) {
        log.info("Processing QR payment for userId={}", userId);

        String payload = request.getQrPayload();

        // 1. Validate CRC16 checksum on server (BR-SPEC-QR03)
        if (!VietQrUtils.validateCrc16(payload)) {
            log.warn("QR Payment rejected: Invalid CRC16 checksum");
            throw new BusinessException(ErrorCode.QR_INVALID_CHECKSUM);
        }

        // 2. Validate PIN (BR-SPEC-SEC01)
        if (!StringUtils.hasText(request.getPin())) {
            throw new BusinessException(ErrorCode.INVALID_PIN_FORMAT, "Mã PIN xác thực là bắt buộc");
        }
        pinService.verifyPin(userId, request.getPin());

        // 3. Resolve QR Code information
        Optional<QrCode> qrOpt = qrCodeRepository.findByPayload(payload);
        UUID destWalletId;
        BigDecimal amount;
        UUID qrCodeId = null;

        if (qrOpt.isPresent()) {
            QrCode qr = qrOpt.get();
            qrCodeId = qr.getId();

            // Check single-use dynamic QR (BR-SPEC-QR02)
            if (Boolean.TRUE.equals(qr.getIsUsed())) {
                log.warn("QR Payment rejected: QR code {} has already been used", qr.getId());
                throw new BusinessException(ErrorCode.QR_ALREADY_USED);
            }

            // Check dynamic QR expiration (BR-SPEC-QR01)
            if (qr.getExpiryTime() != null && qr.getExpiryTime().isBefore(OffsetDateTime.now(ZoneOffset.UTC))) {
                log.warn("QR Payment rejected: QR code {} expired at {}", qr.getId(), qr.getExpiryTime());
                throw new BusinessException(ErrorCode.QR_EXPIRED);
            }

            destWalletId = qr.getWalletId();
            amount = qr.getAmount();
        } else {
            // Static or external VietQR
            String walletIdStr = VietQrUtils.extractWalletId(payload);
            if (walletIdStr == null) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Không thể đọc thông tin ví người nhận từ mã QR");
            }
            try {
                destWalletId = UUID.fromString(walletIdStr);
            } catch (IllegalArgumentException e) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Định dạng ID ví nhận trong mã QR không hợp lệ");
            }

            amount = VietQrUtils.extractAmount(payload);
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Số tiền thanh toán không hợp lệ");
        }

        // 4. Resolve wallets & recipient name
        Wallet sourceWallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WALLET_NOT_FOUND));

        Wallet destWallet = walletRepository.findById(destWalletId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEST_WALLET_NOT_FOUND));

        String recipientName = userRepository.findById(destWallet.getUserId())
                .map(User::getFullName)
                .orElse("Người nhận");

        // 5. Validate Limits (BR-SPEC-TX01, TX02, TX04)
        limitValidationService.validateTransferLimits(sourceWallet, destWallet, amount);

        // 6. Calculate Fee
        BigDecimal fee = feeCalculationService.calculateFee(TransactionType.QR_PAYMENT, amount);

        // 7. Lock Ordering & Redisson MultiLock (BR-GEN-03, BR-GEN-04)
        UUID sourceId = sourceWallet.getId();
        UUID destId = destWallet.getId();
        boolean sourceFirst = sourceId.compareTo(destId) < 0;
        UUID firstId = sourceFirst ? sourceId : destId;
        UUID secondId = sourceFirst ? destId : sourceId;

        RLock lock1 = redissonClient.getLock("wallet:lock:" + firstId);
        RLock lock2 = redissonClient.getLock("wallet:lock:" + secondId);
        RLock multiLock = redissonClient.getMultiLock(lock1, lock2);

        try {
            boolean acquired = multiLock.tryLock(5, 15, TimeUnit.SECONDS);
            if (!acquired) {
                throw new BusinessException(ErrorCode.CONCURRENCY_CONFLICT);
            }

            return transferExecutor.executeQrPaymentInTransaction(
                    sourceId, destId, firstId, secondId,
                    amount, fee, "Thanh toán VietQR", idempotencyKey, qrCodeId, recipientName
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Giao dịch bị gián đoạn");
        } finally {
            if (multiLock.isHeldByCurrentThread()) {
                multiLock.unlock();
            }
        }
    }
}
