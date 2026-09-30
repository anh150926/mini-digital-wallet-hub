# 03 - THIẾT KẾ DATABASE & CLOUD HOSTING

---

## I. LỰA CHỌN NỀN TẢNG CLOUD DATABASE

### 1.1. So sánh các nền tảng PostgreSQL Cloud miễn phí

| Tiêu chí | Neon | Supabase | Railway | Render |
| :--- | :--- | :--- | :--- | :--- |
| **Free Tier Storage** | 512 MB | 500 MB | 1 GB (cộng dồn) | 1 GB |
| **Max Connections** | 100 (pooled) | 60 (pooled) | 25 | 25 |
| **Auto-Sleep** | Có (sau 5 phút idle) | Có (sau 1 tuần) | Không | Không |
| **SSL** | Bắt buộc | Bắt buộc | Bắt buộc | Bắt buộc |
| **Dashboard UI** | ✅ SQL Editor | ✅ Table Editor rất tốt | ✅ Cơ bản | ✅ Cơ bản |
| **Branching DB** | ✅ (Dev/Prod riêng) | ❌ | ❌ | ❌ |
| **Khuyến nghị** | ⭐ **Tốt nhất cho Dev** | ⭐ Tốt (có thêm Auth/Storage) | Tạm | Tạm |

### 1.2. Khuyến nghị: Dùng **Neon.tech** làm DB chính

**Lý do:**
- Connection Pooling tích hợp (PgBouncer) — xử lý tốt nhiều kết nối đồng thời.
- Hỗ trợ **Database Branching** — tạo nhánh DB riêng cho dev/test mà không ảnh hưởng production data.
- SQL Editor trên web cho phép chạy query trực tiếp mà không cần cài pgAdmin.

### 1.3. Redis Cloud: Dùng **Upstash** (Serverless Redis)

| Tiêu chí | Upstash | Redis Cloud |
| :--- | :--- | :--- |
| **Free Tier** | 10.000 commands/ngày | 30 MB storage |
| **Persistent** | ✅ | ✅ |
| **TLS** | ✅ | ✅ |
| **Khuyến nghị** | ⭐ **Tốt cho dev/test** | Tốt cho production |

---

## II. QUY TRÌNH CÀI ĐẶT CLOUD DATABASE

### Bước 1: Đăng ký Neon.tech
1. Truy cập [neon.tech](https://neon.tech) → Sign up bằng GitHub.
2. Tạo Project mới: `digital-wallet`.
3. Chọn Region: `Singapore` (gần Việt Nam nhất).
4. Neon sẽ tự tạo database `neondb` và hiển thị Connection String:
   ```
   postgresql://<user>:<password>@<host>.neon.tech/neondb?sslmode=require
   ```
5. Copy Connection String → Lưu vào file `.env` của Backend.

### Bước 2: Đăng ký Upstash Redis
1. Truy cập [upstash.com](https://upstash.com) → Sign up.
2. Tạo Database Redis mới: Region `Singapore`.
3. Copy Connection URL dạng:
   ```
   rediss://default:<password>@<host>.upstash.io:<port>
   ```
4. Lưu vào file `.env` → Cấu hình Redisson client trong Spring Boot.

### Bước 3: Cấu hình `application-dev.yml`
```yaml
spring:
  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://ep-xxx.ap-southeast-1.aws.neon.tech/neondb?sslmode=require}
    username: ${DATABASE_USERNAME:neondb_owner}
    password: ${DATABASE_PASSWORD:your-password}
    hikari:
      maximum-pool-size: 8
      minimum-idle: 2
      connection-timeout: 10000
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
    show-sql: true
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
```

---

## III. DATABASE SCHEMA CHI TIẾT (FLYWAY MIGRATION)

### Migration V1: `V1__create_core_tables.sql`

```sql
-- ============================================================
-- V1: KHỞI TẠO SCHEMA CỐT LÕI - MINI DIGITAL WALLET
-- ============================================================

-- 1. BẢNG USERS (Tài khoản người dùng)
CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phone_number    VARCHAR(15) UNIQUE NOT NULL,
    full_name       VARCHAR(100) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    pin_hash        VARCHAR(255) NOT NULL,
    public_key      TEXT,                               -- Public Key ECDSA để verify giao dịch
    role            VARCHAR(20) NOT NULL DEFAULT 'USER', -- USER, ADMIN, SUPER_ADMIN, OWNER
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, SUSPENDED, LOCKED
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 2. BẢNG WALLETS (Ví điện tử)
CREATE TABLE wallets (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID UNIQUE NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    balance         NUMERIC(19, 0) NOT NULL DEFAULT 0,  -- VND: đơn vị nguyên (không thập phân)
    currency        VARCHAR(3) NOT NULL DEFAULT 'VND',
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, FROZEN, CLOSED
    version         BIGINT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_wallet_balance_non_negative CHECK (balance >= 0)
);

-- 3. BẢNG TRANSACTIONS (Giao dịch)
CREATE TABLE transactions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    idempotency_key     VARCHAR(64) UNIQUE NOT NULL,
    source_wallet_id    UUID REFERENCES wallets(id),       -- NULL khi Top-up (tiền vào từ bên ngoài)
    dest_wallet_id      UUID REFERENCES wallets(id),       -- NULL khi Withdraw (tiền ra bên ngoài)
    amount              NUMERIC(19, 0) NOT NULL CHECK (amount > 0),
    fee                 NUMERIC(19, 0) NOT NULL DEFAULT 0,
    type                VARCHAR(30) NOT NULL,               -- P2P_TRANSFER, TOP_UP, WITHDRAW, QR_PAYMENT
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, PROCESSING, SUCCESS, FAILED, REVERSED
    request_hash        VARCHAR(64),                        -- SHA-256 hash của request body (chống Idempotency tampering)
    request_signature   TEXT,                               -- Chữ ký ECDSA từ Android client
    error_code          VARCHAR(50),
    description         TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 4. BẢNG LEDGER_ENTRIES (Sổ cái bút toán kế toán kép)
CREATE TABLE ledger_entries (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id      UUID NOT NULL REFERENCES transactions(id),
    wallet_id           UUID NOT NULL REFERENCES wallets(id),
    entry_type          VARCHAR(10) NOT NULL,               -- DEBIT (trừ) hoặc CREDIT (cộng)
    amount              NUMERIC(19, 0) NOT NULL CHECK (amount > 0),
    balance_after       NUMERIC(19, 0) NOT NULL,            -- Snapshot số dư sau bút toán
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_ledger_entry_type CHECK (entry_type IN ('DEBIT', 'CREDIT'))
);

-- 5. BẢNG QR_CODES (Mã VietQR EMVCo)
CREATE TABLE qr_codes (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    wallet_id           UUID NOT NULL REFERENCES wallets(id),
    amount              NUMERIC(19, 0),                     -- NULL cho QR tĩnh (không kèm số tiền)
    order_reference     VARCHAR(50) UNIQUE,
    qr_type             VARCHAR(10) NOT NULL DEFAULT 'DYNAMIC', -- DYNAMIC (1 lần) / STATIC (nhiều lần)
    payload             TEXT NOT NULL,                       -- Chuỗi TLV EMVCo đầy đủ
    expiry_time         TIMESTAMPTZ,                        -- Null cho QR tĩnh
    is_used             BOOLEAN NOT NULL DEFAULT FALSE,
    transaction_id      UUID REFERENCES transactions(id),   -- Liên kết đến giao dịch thanh toán
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 6. BẢNG DEVICE_KEYS (Đăng ký thiết bị & khóa ký số)
CREATE TABLE device_keys (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL REFERENCES users(id),
    device_id           VARCHAR(100) NOT NULL,
    device_name         VARCHAR(100),
    public_key          TEXT NOT NULL,                       -- ECDSA P-256 Public Key (Base64)
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    registered_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_user_device UNIQUE (user_id, device_id)
);

-- 7. BẢNG SYSTEM_CONFIGS (Cấu hình hạn mức, biểu phí — ADM-04)
CREATE TABLE system_configs (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    config_key          VARCHAR(100) UNIQUE NOT NULL,        -- VD: TX_MIN_AMOUNT, TX_MAX_DAILY
    config_value        VARCHAR(255) NOT NULL,
    description         TEXT,
    updated_by          UUID REFERENCES users(id),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================
-- INDEXES (Tăng tốc truy vấn)
-- ============================================================
CREATE INDEX idx_transactions_source ON transactions(source_wallet_id, created_at DESC);
CREATE INDEX idx_transactions_dest ON transactions(dest_wallet_id, created_at DESC);
CREATE INDEX idx_transactions_status ON transactions(status, created_at DESC);
CREATE INDEX idx_ledger_wallet ON ledger_entries(wallet_id, created_at DESC);
CREATE INDEX idx_ledger_transaction ON ledger_entries(transaction_id);
CREATE INDEX idx_qr_codes_wallet ON qr_codes(wallet_id, created_at DESC);
CREATE INDEX idx_qr_codes_order_ref ON qr_codes(order_reference) WHERE order_reference IS NOT NULL;
```

### Migration V2: `V2__seed_system_configs.sql`

```sql
-- ============================================================
-- V2: DỮ LIỆU KHỞI TẠO - HẠN MỨC & BIỂU PHÍ MẶC ĐỊNH
-- ============================================================

INSERT INTO system_configs (config_key, config_value, description) VALUES
('TX_MIN_AMOUNT', '10000', 'Số tiền giao dịch tối thiểu (VNĐ)'),
('TX_MAX_PER_TRANSACTION', '5000000', 'Hạn mức tối đa mỗi giao dịch (VNĐ)'),
('TX_MAX_DAILY', '20000000', 'Hạn mức giao dịch tối đa mỗi ngày (VNĐ)'),
('WALLET_MAX_BALANCE', '100000000', 'Số dư tối đa trong ví (VNĐ)'),
('FEE_WITHDRAW_FIXED', '1100', 'Phí rút tiền cố định (VNĐ)'),
('FEE_WITHDRAW_PERCENT', '0.001', 'Phí rút tiền theo phần trăm (0.1%)'),
('FEE_P2P_TRANSFER', '0', 'Phí chuyển tiền nội bộ (VNĐ)'),
('QR_DYNAMIC_EXPIRY_MINUTES', '15', 'Thời gian hết hạn QR động (phút)'),
('PIN_MAX_FAILED_ATTEMPTS', '5', 'Số lần nhập sai PIN tối đa trước khi khóa'),
('PIN_LOCK_DURATION_MINUTES', '15', 'Thời gian tạm khóa khi nhập sai PIN quá giới hạn (phút)');
```

---

## IV. SƠ ĐỒ QUAN HỆ (ER DIAGRAM)

```mermaid
erDiagram
    USERS ||--|| WALLETS : "sở hữu (1:1)"
    USERS ||--o{ DEVICE_KEYS : "đăng ký thiết bị"
    WALLETS ||--o{ TRANSACTIONS : "nguồn (source)"
    WALLETS ||--o{ TRANSACTIONS : "đích (dest)"
    WALLETS ||--o{ QR_CODES : "tạo mã QR"
    WALLETS ||--o{ LEDGER_ENTRIES : "bút toán"
    TRANSACTIONS ||--o{ LEDGER_ENTRIES : "chi tiết bút toán"
    TRANSACTIONS ||--o| QR_CODES : "thanh toán QR"

    USERS {
        UUID id PK
        VARCHAR phone_number UK
        VARCHAR full_name
        VARCHAR password_hash
        VARCHAR pin_hash
        TEXT public_key
        VARCHAR role
        VARCHAR status
    }

    WALLETS {
        UUID id PK
        UUID user_id FK_UK
        NUMERIC balance
        VARCHAR currency
        VARCHAR status
        BIGINT version
    }

    TRANSACTIONS {
        UUID id PK
        VARCHAR idempotency_key UK
        UUID source_wallet_id FK
        UUID dest_wallet_id FK
        NUMERIC amount
        NUMERIC fee
        VARCHAR type
        VARCHAR status
        VARCHAR request_hash
    }

    LEDGER_ENTRIES {
        UUID id PK
        UUID transaction_id FK
        UUID wallet_id FK
        VARCHAR entry_type
        NUMERIC amount
        NUMERIC balance_after
    }

    QR_CODES {
        UUID id PK
        UUID wallet_id FK
        NUMERIC amount
        VARCHAR order_reference UK
        VARCHAR qr_type
        TEXT payload
        BOOLEAN is_used
    }

    DEVICE_KEYS {
        UUID id PK
        UUID user_id FK
        VARCHAR device_id
        TEXT public_key
        BOOLEAN is_active
    }

    SYSTEM_CONFIGS {
        UUID id PK
        VARCHAR config_key UK
        VARCHAR config_value
        TEXT description
    }
```

---

## V. CHIẾN LƯỢC QUẢN LÝ MIGRATION (FLYWAY)

### Quy tắc đặt tên file Migration:
```
V<version>__<mô_tả_snake_case>.sql
```

| File | Mô tả |
| :--- | :--- |
| `V1__create_core_tables.sql` | Khởi tạo toàn bộ bảng cốt lõi |
| `V2__seed_system_configs.sql` | Dữ liệu khởi tạo hạn mức & biểu phí |
| `V3__add_audit_columns.sql` | Bổ sung cột audit (nếu cần) |
| `V4__create_admin_user.sql` | Tạo tài khoản Admin mặc định |

### Lưu ý quan trọng khi dùng Cloud DB + Flyway:
- Flyway migration chạy **một lần duy nhất** — không bao giờ sửa file migration đã chạy.
- Nếu cần thay đổi schema, tạo file migration **mới** (VD: `V3__alter_xxx.sql`).
- Set `spring.jpa.hibernate.ddl-auto=validate` — Hibernate chỉ **kiểm tra** schema khớp với Entity, KHÔNG TỰ SỬA DB.
