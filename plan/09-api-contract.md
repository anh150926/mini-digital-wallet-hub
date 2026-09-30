# 09 - HỢP ĐỒNG API & ENDPOINT CATALOG (API CONTRACT)

---

## I. QUY ƯỚC CHUNG

### Base URL:
```
Development:  http://localhost:8080/api/v1
Production:   https://api.your-domain.com/api/v1
```

### Headers bắt buộc (cho mọi request cần xác thực):
| Header | Mô Tả | Bắt buộc |
| :--- | :--- | :---: |
| `Authorization` | `Bearer <access_token>` | ✅ |
| `Content-Type` | `application/json` | ✅ |
| `X-Device-Id` | ID thiết bị (Android) | ✅ (Mobile) |
| `Idempotency-Key` | UUID v4 (cho request ghi tài chính) | ✅ (TX, QR Pay) |

### Response Format chuẩn:
```json
// Thành công
{
  "success": true,
  "data": { ... },
  "timestamp": "2026-10-01T14:30:00Z"
}

// Lỗi
{
  "success": false,
  "error": {
    "code": "INSUFFICIENT_FUNDS",
    "message": "Số dư ví không đủ để thực hiện giao dịch",
    "details": { "current_balance": 120000 }
  },
  "timestamp": "2026-10-01T14:30:00Z"
}
```

---

## II. ENDPOINT CATALOG

### Module AUTH

#### `POST /api/v1/auth/register` — Đăng ký tài khoản (AUTH-01)
**Actor:** GUEST
**Rules:** BR-SPEC-AUTH01, BR-SPEC-AUTH02, BR-SPEC-AUTH03
```
Request Body:
{
  "phone_number": "0987654321",
  "full_name": "Nguyễn Văn A",
  "password": "MyP@ss123",
  "pin": "123456"
}

Response 201 Created:
{
  "success": true,
  "data": {
    "user_id": "uuid",
    "wallet_id": "uuid",
    "phone_number": "0987654321"
  }
}

Errors: 400 INVALID_PHONE_FORMAT, 400 WEAK_PASSWORD, 409 PHONE_ALREADY_EXISTS
```

#### `POST /api/v1/auth/login` — Đăng nhập (AUTH-02)
**Actor:** GUEST
```
Request Body:
{
  "phone_number": "0987654321",
  "password": "MyP@ss123",
  "device_id": "android-abc-123"
}

Response 200 OK:
{
  "success": true,
  "data": {
    "access_token": "eyJhbGc...",
    "refresh_token": "eyJhbGc...",
    "expires_in": 900,
    "user": {
      "id": "uuid",
      "full_name": "Nguyễn Văn A",
      "phone_number": "0987654321",
      "role": "USER"
    }
  }
}

Errors: 401 INVALID_CREDENTIALS, 423 ACCOUNT_LOCKED
```

#### `POST /api/v1/auth/refresh` — Làm mới Access Token (AUTH-02)
**Actor:** USER
```
Request Body:
{
  "refresh_token": "eyJhbGc..."
}

Response 200 OK:
{
  "success": true,
  "data": {
    "access_token": "eyJhbGc...(new)",
    "expires_in": 900
  }
}

Errors: 401 REFRESH_TOKEN_EXPIRED
```

---

### Module WALLET

#### `GET /api/v1/wallets/me` — Xem số dư ví (WAL-01)
**Actor:** USER
```
Response 200 OK:
{
  "success": true,
  "data": {
    "wallet_id": "uuid",
    "balance": 1500000,
    "currency": "VND",
    "status": "ACTIVE"
  }
}
```

#### `GET /api/v1/wallets/me/transactions` — Lịch sử giao dịch (WAL-02)
**Actor:** USER
```
Query Params: ?page=0&size=20&type=P2P_TRANSFER&status=SUCCESS

Response 200 OK:
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "uuid",
        "type": "P2P_TRANSFER",
        "status": "SUCCESS",
        "amount": 500000,
        "fee": 0,
        "direction": "OUTGOING",
        "counterparty_name": "Trần Thị B",
        "counterparty_phone": "0912345678",
        "description": "Tiền ăn trưa",
        "created_at": "2026-10-01T14:30:00Z"
      }
    ],
    "page": 0,
    "size": 20,
    "total_elements": 45,
    "total_pages": 3
  }
}
```

---

### Module TRANSACTION

#### `POST /api/v1/transfers` — Chuyển tiền P2P (TX-01)
**Actor:** USER
**Rules:** BR-GEN-01~05, BR-SPEC-TX01~TX04, BR-SPEC-SEC01
**Headers bổ sung:** `Idempotency-Key: uuid-v4`
```
Request Body:
{
  "dest_phone_number": "0912345678",
  "amount": 500000,
  "description": "Thanh toán tiền điện",
  "pin_hash": "sha256(pin+nonce+timestamp)",
  "nonce": "random-string",
  "timestamp": 1696171800000,
  "signature": "base64-ecdsa-signature"
}

Response 200 OK:
{
  "success": true,
  "data": {
    "transaction_id": "uuid",
    "status": "SUCCESS",
    "amount": 500000,
    "fee": 0,
    "source_balance_after": 1000000,
    "created_at": "2026-10-01T14:30:00Z"
  }
}

Errors:
  422 INSUFFICIENT_FUNDS
  422 BELOW_MINIMUM_AMOUNT
  422 EXCEEDS_PER_TRANSACTION_LIMIT
  422 EXCEEDS_DAILY_LIMIT
  422 SELF_TRANSFER_NOT_ALLOWED
  422 IDEMPOTENCY_PAYLOAD_MISMATCH
  403 WALLET_FROZEN
  409 DUPLICATE_REQUEST
  409 CONCURRENCY_CONFLICT
  401 INVALID_PIN
  423 ACCOUNT_LOCKED
  404 DEST_WALLET_NOT_FOUND
```

#### `POST /api/v1/topup` — Nạp tiền (TX-02)
**Actor:** USER
```
Request Body:
{
  "amount": 1000000,
  "payment_method": "SIMULATED_BANK"
}

Response 200 OK:
{
  "success": true,
  "data": {
    "transaction_id": "uuid",
    "status": "SUCCESS",
    "new_balance": 2500000
  }
}
```

---

### Module QR CODE

#### `POST /api/v1/qr-codes` — Sinh mã VietQR (QR-01)
**Actor:** USER
```
Request Body:
{
  "amount": 200000,
  "description": "Thanh toán đơn hàng #123"
}

Response 201 Created:
{
  "success": true,
  "data": {
    "qr_id": "uuid",
    "payload": "00020101021238570010A00000072701...",
    "qr_type": "DYNAMIC",
    "amount": 200000,
    "expiry_time": "2026-10-01T14:45:00Z"
  }
}
```

#### `POST /api/v1/qr-codes/pay` — Thanh toán qua QR (QR-03)
**Actor:** USER
**Headers bổ sung:** `Idempotency-Key: uuid-v4`
```
Request Body:
{
  "qr_payload": "00020101021238570010A00000072701...",
  "pin_hash": "sha256(pin+nonce+timestamp)",
  "nonce": "random",
  "timestamp": 1696171800000,
  "signature": "base64-ecdsa-signature"
}

Response 200 OK:
{
  "success": true,
  "data": {
    "transaction_id": "uuid",
    "status": "SUCCESS",
    "amount": 200000,
    "recipient_name": "Trần Thị B",
    "created_at": "2026-10-01T14:30:00Z"
  }
}

Errors: 410 QR_EXPIRED, 410 QR_ALREADY_USED, 422 QR_INVALID_CHECKSUM
        (+ tất cả lỗi của Transfer)
```

---

### Module ADMIN

#### `GET /api/v1/admin/dashboard` — Tổng quan (ADM-01)
**Actor:** ADMIN, SUPER_ADMIN, OWNER
```
Response 200 OK:
{
  "success": true,
  "data": {
    "total_users": 1250,
    "total_active_wallets": 1180,
    "today_transaction_count": 342,
    "today_transaction_volume": 125000000,
    "daily_stats": [
      { "date": "2026-09-28", "count": 300, "volume": 110000000 },
      { "date": "2026-09-27", "count": 280, "volume": 98000000 }
    ]
  }
}
```

#### `PUT /api/v1/admin/wallets/{walletId}/freeze` — Khóa ví (ADM-02)
**Actor:** ADMIN, SUPER_ADMIN
```
Request Body:
{ "reason": "Nghi ngờ giao dịch bất thường" }

Response 200 OK:
{
  "success": true,
  "data": { "wallet_id": "uuid", "status": "FROZEN" }
}
```

#### `GET /api/v1/admin/reconciliation` — Đối soát (ADM-03)
**Actor:** ADMIN, SUPER_ADMIN, OWNER
```
Query Params: ?from=2026-10-01&to=2026-10-31&type=P2P_TRANSFER

Response 200 OK:
{
  "success": true,
  "data": {
    "total_debit": 45000000,
    "total_credit": 45000000,
    "net_balance": 0,
    "is_balanced": true,
    "transaction_count": 890
  }
}
```

#### `GET /api/v1/admin/configs` — Lấy cấu hình (ADM-04)
#### `PUT /api/v1/admin/configs/{key}` — Cập nhật cấu hình (ADM-04)
**Actor:** SUPER_ADMIN, OWNER
```
PUT Request Body:
{ "value": "10000000", "reason": "Tăng hạn mức chuyển tiền tối đa" }
```

---

### Module DEVICE/SECURITY

#### `POST /api/v1/devices/register-key` — Đăng ký Public Key (SEC-01)
**Actor:** USER
```
Request Body:
{
  "device_id": "android-abc-123",
  "device_name": "Samsung Galaxy S24",
  "public_key": "MFkwEwYHKoZIzj0CAQYIKoZI..." 
}

Response 201 Created:
{
  "success": true,
  "data": { "device_id": "android-abc-123", "registered": true }
}
```
