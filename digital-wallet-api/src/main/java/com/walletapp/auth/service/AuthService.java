package com.walletapp.auth.service;

import com.walletapp.auth.dto.*;
import com.walletapp.auth.repository.UserRepository;
import com.walletapp.common.enums.ErrorCode;
import com.walletapp.common.enums.Role;
import com.walletapp.common.enums.UserStatus;
import com.walletapp.common.enums.WalletStatus;
import com.walletapp.common.exception.BusinessException;
import com.walletapp.entity.User;
import com.walletapp.security.service.JwtService;
import com.walletapp.wallet.entity.Wallet;
import com.walletapp.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        log.info("Processing registration for phone number: {}", request.getPhoneNumber());

        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            log.warn("Registration rejected - phone number already exists: {}", request.getPhoneNumber());
            throw new BusinessException(ErrorCode.PHONE_ALREADY_EXISTS);
        }

        // Hash password and PIN with BCrypt
        String passwordHash = passwordEncoder.encode(request.getPassword());
        String pinHash = passwordEncoder.encode(request.getPin());

        // 1. Create and save User
        User user = User.builder()
                .phoneNumber(request.getPhoneNumber())
                .fullName(request.getFullName())
                .passwordHash(passwordHash)
                .pinHash(pinHash)
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build();
        user = userRepository.save(user);

        // 2. Automatically create matching Wallet in the same transaction (BR-SPEC-AUTH03)
        Wallet wallet = Wallet.builder()
                .userId(user.getId())
                .balance(BigDecimal.ZERO)
                .currency("VND")
                .status(WalletStatus.ACTIVE)
                .build();
        wallet = walletRepository.save(wallet);

        log.info("Registration successful for user_id={}, wallet_id={}", user.getId(), wallet.getId());

        return RegisterResponse.builder()
                .userId(user.getId())
                .walletId(wallet.getId())
                .phoneNumber(user.getPhoneNumber())
                .build();
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        log.info("Processing login for phone number: {}", request.getPhoneNumber());

        User user = userRepository.findByPhoneNumber(request.getPhoneNumber())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Login failed - invalid password for phone number: {}", request.getPhoneNumber());
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            log.warn("Login blocked - account locked for phone number: {}", request.getPhoneNumber());
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED, "Tài khoản của bạn đã bị khóa hoặc tạm ngưng");
        }

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtService.getAccessTokenExpirationSeconds())
                .user(AuthResponse.UserSummary.builder()
                        .id(user.getId())
                        .fullName(user.getFullName())
                        .phoneNumber(user.getPhoneNumber())
                        .role(user.getRole().name())
                        .build())
                .build();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> refreshToken(RefreshTokenRequest request) {
        String token = request.getRefreshToken();

        if (!jwtService.validateToken(token)) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_EXPIRED, "Refresh token không hợp lệ hoặc đã hết hạn");
        }

        String tokenType = jwtService.extractTokenType(token);
        if (!"REFRESH".equals(tokenType)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "Token cung cấp không phải là refresh token");
        }

        UUID userId = jwtService.extractUserId(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }

        String newAccessToken = jwtService.generateAccessToken(user);
        return Map.of(
                "access_token", newAccessToken,
                "expires_in", jwtService.getAccessTokenExpirationSeconds()
        );
    }
}
