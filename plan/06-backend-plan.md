# 06 - KẾ HOẠCH BACKEND SPRING BOOT (JAVA 21)

---

## I. THÔNG TIN DỰ ÁN

| Thuộc tính | Giá trị |
| :--- | :--- |
| **Group** | `com.walletapp` |
| **Artifact** | `digital-wallet-api` |
| **Java Version** | 21 (LTS) |
| **Spring Boot** | 3.3.x |
| **Build Tool** | Maven |
| **Port** | 8080 |

---

## II. DEPENDENCIES (pom.xml)

```xml
<!-- Core -->
spring-boot-starter-web
spring-boot-starter-data-jpa
spring-boot-starter-security
spring-boot-starter-validation

<!-- Database -->
postgresql                           (PostgreSQL JDBC Driver)
flyway-core                          (Database Migration)
flyway-database-postgresql

<!-- Redis & Distributed Lock -->
spring-boot-starter-data-redis
redisson-spring-boot-starter         (Redisson Client — MultiLock, Watchdog)

<!-- Security -->
io.jsonwebtoken:jjwt-api             (JWT sinh/verify token)
io.jsonwebtoken:jjwt-impl
io.jsonwebtoken:jjwt-jackson
org.bouncycastle:bcprov-jdk18on      (Verify ECDSA Signature từ Android)

<!-- Documentation -->
springdoc-openapi-starter-webmvc-ui  (Swagger UI auto-gen)

<!-- Utils -->
lombok
spring-boot-starter-test
```

---

## III. PACKAGE STRUCTURE CHI TIẾT

```
src/main/java/com/walletapp/
│
├── DigitalWalletApiApplication.java        ← Main class
│
├── config/
│   ├── SecurityConfig.java                 ← Spring Security Filter Chain
│   ├── RedisConfig.java                    ← Redisson Client Bean
│   ├── CorsConfig.java                     ← CORS cho Next.js & Android
│   ├── OpenApiConfig.java                  ← Swagger/OpenAPI metadata
│   └── JacksonConfig.java                 ← ObjectMapper (UTC timezone, etc.)
│
├── common/
│   ├── dto/
│   │   ├── ApiResponse.java               ← Response wrapper: {success, data, error}
│   │   └── PageResponse.java              ← Response phân trang
│   ├── exception/
│   │   ├── BusinessException.java         ← Exception nghiệp vụ (kèm error code)
│   │   ├── ConcurrencyException.java      ← Không lấy được lock
│   │   └── GlobalExceptionHandler.java    ← @ControllerAdvice xử lý tập trung
│   ├── filter/
│   │   ├── IdempotencyFilter.java         ← Servlet Filter kiểm tra Idempotency-Key
│   │   └── JwtAuthenticationFilter.java   ← Filter xác thực JWT token
│   └── util/
│       ├── CryptoUtils.java               ← SHA-256, ECDSA Verify, CRC16
│       └── VietQrUtils.java               ← TLV Builder, QR Payload generator
│
├── auth/
│   ├── controller/AuthController.java     ← POST /auth/register, /auth/login
│   ├── dto/
│   │   ├── RegisterRequest.java
│   │   ├── LoginRequest.java
│   │   └── AuthResponse.java
│   └── service/AuthService.java           ← Hash password, sinh JWT, tạo ví
│
├── wallet/
│   ├── controller/WalletController.java   ← GET /wallets/me, GET /wallets/me/history
│   ├── dto/
│   │   ├── WalletResponse.java
│   │   └── TransactionHistoryResponse.java
│   ├── entity/Wallet.java                 ← JPA Entity — @Version cho Optimistic Lock
│   ├── repository/WalletRepository.java   ← Custom query: findByIdWithPessimisticLock
│   └── service/WalletQueryService.java    ← Truy vấn số dư, lịch sử
│
├── transaction/
│   ├── controller/TransferController.java ← POST /transfers
│   ├── dto/
│   │   ├── TransferRequest.java           ← {destPhoneNumber, amount, pin}
│   │   └── TransferResponse.java
│   ├── entity/
│   │   ├── Transaction.java
│   │   └── LedgerEntry.java
│   ├── repository/
│   │   ├── TransactionRepository.java
│   │   └── LedgerEntryRepository.java
│   └── service/
│       ├── TransferOrchestrator.java      ← Giữ Redis Lock, gọi inner service
│       ├── TransferExecutor.java          ← @Transactional: lock DB, cộng/trừ, ghi ledger
│       ├── FeeCalculationService.java     ← Tính phí theo system_configs
│       └── LimitValidationService.java    ← Kiểm tra hạn mức (BR-SPEC-TX01)
│
├── qrcode/
│   ├── controller/QrCodeController.java   ← POST /qr-codes, POST /qr-codes/pay
│   ├── dto/
│   │   ├── CreateQrRequest.java
│   │   ├── QrPaymentRequest.java
│   │   └── QrCodeResponse.java
│   ├── entity/QrCode.java
│   ├── repository/QrCodeRepository.java
│   └── service/
│       ├── QrCodeGeneratorService.java    ← Sinh TLV payload EMVCo
│       └── QrCodePaymentService.java      ← Xử lý thanh toán từ QR
│
├── security/
│   ├── controller/DeviceKeyController.java ← POST /devices/register-key
│   ├── entity/DeviceKey.java
│   ├── repository/DeviceKeyRepository.java
│   ├── service/
│   │   ├── PinService.java                ← Verify PIN, quản lý fail counter (Redis)
│   │   ├── SignatureVerifyService.java     ← Verify ECDSA signature từ Android
│   │   └── JwtService.java                ← Sinh/Verify/Refresh JWT token
│   └── dto/
│       └── RegisterDeviceKeyRequest.java
│
├── admin/
│   ├── controller/
│   │   ├── AdminDashboardController.java  ← GET /admin/dashboard
│   │   ├── AdminUserController.java       ← GET/PUT /admin/users, /admin/wallets/freeze
│   │   ├── AdminReconcileController.java  ← GET /admin/reconciliation
│   │   └── AdminConfigController.java     ← GET/PUT /admin/configs
│   ├── dto/ (các DTO riêng cho admin)
│   └── service/
│       ├── DashboardService.java
│       ├── UserManagementService.java
│       ├── ReconciliationService.java
│       └── SystemConfigService.java
│
└── entity/
    ├── User.java                          ← JPA Entity (users table)
    └── SystemConfig.java                  ← JPA Entity (system_configs table)
```

---

## IV. MẪU CODE CỐT LÕI

### 4.1. TransferOrchestrator — Bọc Lock ngoài Transaction

```java
@Service
@RequiredArgsConstructor
public class TransferOrchestrator {

    private final RedissonClient redissonClient;
    private final TransferExecutor transferExecutor;  // Chứa @Transactional

    public TransferResponse execute(UUID userId, TransferRequest request) {
        UUID sourceId = /* resolve từ userId */;
        UUID destId   = /* resolve từ destPhoneNumber */;

        // Lock Ordering: luôn lock ID nhỏ trước
        boolean sourceFirst = sourceId.compareTo(destId) < 0;
        UUID firstId  = sourceFirst ? sourceId : destId;
        UUID secondId = sourceFirst ? destId   : sourceId;

        RLock lock1 = redissonClient.getLock("wallet:lock:" + firstId);
        RLock lock2 = redissonClient.getLock("wallet:lock:" + secondId);
        RLock multiLock = redissonClient.getMultiLock(lock1, lock2);

        try {
            boolean acquired = multiLock.tryLock(3, 10, TimeUnit.SECONDS);
            if (!acquired) throw new ConcurrencyException("CONCURRENCY_CONFLICT");

            // Gọi @Transactional SAU KHI có lock
            return transferExecutor.executeInTransaction(
                sourceId, destId, firstId, secondId, request
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("SYSTEM_ERROR");
        } finally {
            if (multiLock.isHeldByCurrentThread()) {
                multiLock.unlock();  // Nhả lock SAU KHI DB đã commit
            }
        }
    }
}
```

### 4.2. WalletRepository — Pessimistic Write Lock

```java
public interface WalletRepository extends JpaRepository<Wallet, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.id = :id")
    Optional<Wallet> findByIdWithPessimisticLock(@Param("id") UUID id);

    @Query("SELECT w FROM Wallet w WHERE w.userId = :userId")
    Optional<Wallet> findByUserId(@Param("userId") UUID userId);
}
```

---

## V. CẤU HÌNH MÔI TRƯỜNG

### application.yml (Base):
```yaml
server:
  port: 8080

spring:
  profiles:
    active: ${APP_ENV:dev}
  threads:
    virtual:
      enabled: true
  jackson:
    time-zone: UTC
    serialization:
      write-dates-as-timestamps: false
```

### application-dev.yml:
```yaml
spring:
  datasource:
    url: ${DATABASE_URL}
    username: ${DATABASE_USERNAME}
    password: ${DATABASE_PASSWORD}
    hikari:
      maximum-pool-size: 8
      minimum-idle: 2
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: true
  flyway:
    enabled: true
    baseline-on-migrate: true

logging:
  level:
    com.walletapp: DEBUG
    org.hibernate.SQL: DEBUG
```

### application-prod.yml:
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
  jpa:
    show-sql: false

logging:
  level:
    com.walletapp: INFO
    org.hibernate.SQL: WARN
```
