package com.walletapp.security.service;

import com.walletapp.auth.repository.UserRepository;
import com.walletapp.common.enums.ErrorCode;
import com.walletapp.common.exception.BusinessException;
import com.walletapp.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PinService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_MINUTES = 15;

    public void verifyPin(UUID userId, String rawPin) {
        String lockKey = "pin_lock:" + userId;
        String failKey = "pin_fail:" + userId;

        // 1. Check if user is currently locked due to too many failed PIN attempts
        if (Boolean.TRUE.equals(redisTemplate.hasKey(lockKey))) {
            Long ttl = redisTemplate.getExpire(lockKey);
            long remainingSeconds = (ttl != null && ttl > 0) ? ttl : 60;
            log.warn("PIN verification rejected - user {} is temporarily locked for {}s", userId, remainingSeconds);
            throw new BusinessException(
                    ErrorCode.ACCOUNT_LOCKED,
                    "Tài khoản đang bị tạm khóa giao dịch do nhập sai PIN. Vui lòng thử lại sau " + (remainingSeconds / 60 + 1) + " phút",
                    java.util.Map.of("retry_after_seconds", remainingSeconds)
            );
        }

        // 2. Fetch User and check PIN
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        boolean matches = passwordEncoder.matches(rawPin, user.getPinHash());

        if (matches) {
            // Reset fail counter on success
            redisTemplate.delete(failKey);
            log.info("PIN verification successful for user {}", userId);
        } else {
            Long failedCount = redisTemplate.opsForValue().increment(failKey);
            if (failedCount == null) {
                failedCount = 1L;
            }
            if (failedCount == 1) {
                redisTemplate.expire(failKey, Duration.ofMinutes(LOCK_MINUTES));
            }

            log.warn("Incorrect PIN attempt {}/{} for user {}", failedCount, MAX_FAILED_ATTEMPTS, userId);

            if (failedCount >= MAX_FAILED_ATTEMPTS) {
                redisTemplate.opsForValue().set(lockKey, "LOCKED", Duration.ofMinutes(LOCK_MINUTES));
                redisTemplate.delete(failKey);
                throw new BusinessException(
                        ErrorCode.ACCOUNT_LOCKED,
                        "Bạn đã nhập sai PIN 5 lần. Tài khoản bị tạm khóa giao dịch 15 phút"
                );
            } else {
                long remaining = MAX_FAILED_ATTEMPTS - failedCount;
                throw new BusinessException(
                        ErrorCode.INVALID_PIN,
                        "Mã PIN không chính xác. Bạn còn " + remaining + " lần thử"
                );
            }
        }
    }
}
