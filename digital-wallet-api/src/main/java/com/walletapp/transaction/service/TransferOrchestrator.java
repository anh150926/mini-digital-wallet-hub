package com.walletapp.transaction.service;

import com.walletapp.auth.repository.UserRepository;
import com.walletapp.common.enums.ErrorCode;
import com.walletapp.common.enums.TransactionType;
import com.walletapp.common.exception.BusinessException;
import com.walletapp.entity.User;
import com.walletapp.security.service.PinService;
import com.walletapp.transaction.dto.TopupRequest;
import com.walletapp.transaction.dto.TopupResponse;
import com.walletapp.transaction.dto.TransferRequest;
import com.walletapp.transaction.dto.TransferResponse;
import com.walletapp.wallet.entity.Wallet;
import com.walletapp.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferOrchestrator {

    private final RedissonClient redissonClient;
    private final TransferExecutor transferExecutor;
    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final PinService pinService;
    private final LimitValidationService limitValidationService;
    private final FeeCalculationService feeCalculationService;

    public TransferResponse executeTransfer(UUID userId, TransferRequest request, String idempotencyKey) {
        log.info("Initiating P2P transfer request by userId={}, destPhone={}, amount={}",
                userId, request.getDestPhoneNumber(), request.getAmount());

        // 1. PIN verification (BR-SPEC-SEC01)
        if (!StringUtils.hasText(request.getPin())) {
            throw new BusinessException(ErrorCode.INVALID_PIN_FORMAT, "Mã PIN xác thực là bắt buộc");
        }
        pinService.verifyPin(userId, request.getPin());

        // 2. Resolve source wallet
        Wallet sourceWallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WALLET_NOT_FOUND));

        // 3. Resolve destination user and wallet
        User destUser = userRepository.findByPhoneNumber(request.getDestPhoneNumber())
                .orElseThrow(() -> new BusinessException(ErrorCode.DEST_WALLET_NOT_FOUND));
        Wallet destWallet = walletRepository.findByUserId(destUser.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.DEST_WALLET_NOT_FOUND));

        // 4. Validate Transfer Limits and Rules (BR-SPEC-TX01, TX02, TX04)
        limitValidationService.validateTransferLimits(sourceWallet, destWallet, request.getAmount());

        // 5. Calculate Fee (BR-SPEC-TX03)
        BigDecimal fee = feeCalculationService.calculateFee(TransactionType.P2P_TRANSFER, request.getAmount());

        // 6. Lock Ordering to prevent Distributed Deadlocks (BR-GEN-03, BR-GEN-04)
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
                log.warn("Could not acquire multiLock for wallets {} and {}", firstId, secondId);
                throw new BusinessException(ErrorCode.CONCURRENCY_CONFLICT);
            }

            // Call @Transactional DB execution outside lock scope (BR-GEN-04)
            return transferExecutor.executeTransferInTransaction(
                    sourceId, destId, firstId, secondId,
                    request.getAmount(), fee, request.getDescription(), idempotencyKey
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Giao dịch bị gián đoạn, vui lòng thử lại");
        } finally {
            if (multiLock.isHeldByCurrentThread()) {
                multiLock.unlock();
            }
        }
    }

    public TopupResponse executeTopup(UUID userId, TopupRequest request, String idempotencyKey) {
        log.info("Initiating topup for userId={}, amount={}", userId, request.getAmount());

        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WALLET_NOT_FOUND));

        RLock lock = redissonClient.getLock("wallet:lock:" + wallet.getId());

        try {
            boolean acquired = lock.tryLock(5, 15, TimeUnit.SECONDS);
            if (!acquired) {
                throw new BusinessException(ErrorCode.CONCURRENCY_CONFLICT);
            }

            return transferExecutor.executeTopupInTransaction(
                    wallet.getId(), request.getAmount(), idempotencyKey, request.getPaymentMethod()
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Giao dịch bị gián đoạn");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
