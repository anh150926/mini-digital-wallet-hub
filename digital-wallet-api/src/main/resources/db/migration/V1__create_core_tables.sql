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
