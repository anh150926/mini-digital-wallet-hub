package com.walletapp.admin.config;

import com.walletapp.auth.repository.UserRepository;
import com.walletapp.common.enums.Role;
import com.walletapp.common.enums.UserStatus;
import com.walletapp.common.enums.WalletStatus;
import com.walletapp.entity.User;
import com.walletapp.wallet.entity.Wallet;
import com.walletapp.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final PasswordEncoder passwordEncoder;

    public static final String ADMIN_PHONE = "0999999999";
    public static final String ADMIN_DEFAULT_PASSWORD = "Admin@123";
    public static final String ADMIN_DEFAULT_PIN = "123456";

    @Override
    @Transactional
    public void run(String... args) {
        if (!userRepository.existsByPhoneNumber(ADMIN_PHONE)) {
            log.info("Khởi tạo tài khoản Administrator mặc định: {}", ADMIN_PHONE);
            User admin = User.builder()
                    .phoneNumber(ADMIN_PHONE)
                    .fullName("Quản Trị Viên Hệ Thống")
                    .passwordHash(passwordEncoder.encode(ADMIN_DEFAULT_PASSWORD))
                    .pinHash(passwordEncoder.encode(ADMIN_DEFAULT_PIN))
                    .role(Role.ADMIN)
                    .status(UserStatus.ACTIVE)
                    .build();

            User savedAdmin = userRepository.save(admin);

            Wallet adminWallet = Wallet.builder()
                    .userId(savedAdmin.getId())
                    .balance(new BigDecimal("100000000")) // 100,000,000 VND
                    .currency("VND")
                    .status(WalletStatus.ACTIVE)
                    .build();

            walletRepository.save(adminWallet);
            log.info("Tài khoản Administrator và ví quản trị đã được tạo thành công.");
        } else {
            log.info("Tài khoản Administrator {} đã tồn tại.", ADMIN_PHONE);
        }
    }
}
