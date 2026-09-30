# 05 - LUỒNG NGHIỆP VỤ END-TO-END (BUSINESS FLOWS)

---

## I. DANH MỤC LUỒNG

| Mã Luồng | Tên | Tác nhân chính | Chức năng liên quan |
| :--- | :--- | :--- | :--- |
| `FLOW-01` | Đăng ký tài khoản & Kích hoạt ví | GUEST → USER | AUTH-01 |
| `FLOW-02` | Đăng nhập & Cấp JWT | GUEST → USER | AUTH-02 |
| `FLOW-03` | Chuyển tiền P2P (Full Pipeline) | USER + SYSTEM | TX-01, SEC-02 |
| `FLOW-04` | Nạp tiền vào ví (Top-up) | USER + SYSTEM | TX-02 |
| `FLOW-05` | Rút tiền về ngân hàng | USER + SYSTEM | TX-03 |
| `FLOW-06` | Tạo mã VietQR | USER + SYSTEM | QR-01 |
| `FLOW-07` | Quét & Thanh toán QR | USER + SYSTEM | QR-02, QR-03, TX-01 |
| `FLOW-08` | Admin khóa ví khẩn cấp | ADMIN + SYSTEM | ADM-02 |
| `FLOW-09` | Đối soát sổ cái | ADMIN + SYSTEM | ADM-03 |

---

## II. CHI TIẾT TỪNG LUỒNG

---

### FLOW-01: Đăng Ký Tài Khoản & Kích Hoạt Ví

**Tác nhân:** GUEST → SYSTEM
**Quy tắc áp dụng:** BR-SPEC-AUTH01, BR-SPEC-AUTH02, BR-SPEC-AUTH03

```
GUEST (Android App)                      SYSTEM (Backend)                     DATABASE
       │                                       │                                │
       │  1. Nhập SĐT, Tên, Mật khẩu, PIN     │                                │
       │──────────────────────────────────────>│                                │
       │                                       │  2. Validate:                  │
       │                                       │     - SĐT regex ^0[3-9]\d{8}$  │
       │                                       │     - Mật khẩu >= 8 ký tự      │
       │                                       │     - PIN đúng 6 số            │
       │                                       │                                │
       │                                       │  3. Kiểm tra SĐT trùng        │
       │                                       │──────────────────────────────>│
       │                                       │  ← SELECT users WHERE phone    │
       │                                       │                                │
       │                                       │  4. @Transactional:            │
       │                                       │     - Hash password (BCrypt)    │
       │                                       │     - Hash PIN (BCrypt)         │
       │                                       │     - INSERT users              │
       │                                       │     - INSERT wallets            │
       │                                       │       (balance=0, status=ACTIVE)│
       │                                       │──────────────────────────────>│
       │                                       │  ← COMMIT                      │
       │                                       │                                │
       │  5. Response: HTTP 201 Created        │                                │
       │  {userId, walletId, phoneNumber}      │                                │
       │<──────────────────────────────────────│                                │
```

---

### FLOW-02: Đăng Nhập & Cấp JWT

**Tác nhân:** GUEST → USER
**Quy tắc áp dụng:** BR-SPEC-SEC01 (Brute-force), BR-SPEC-SEC02 (Device Binding)

```
GUEST (Android App)                      SYSTEM (Backend)              REDIS         DATABASE
       │                                       │                        │               │
       │  1. POST /api/v1/auth/login           │                        │               │
       │  {phone, password, deviceId}          │                        │               │
       │──────────────────────────────────────>│                        │               │
       │                                       │  2. Query user by phone│               │
       │                                       │──────────────────────────────────────>│
       │                                       │  ← User entity                        │
       │                                       │                                        │
       │                                       │  3. Verify BCrypt password              │
       │                                       │                                        │
       │                                       │  4. Kiểm tra login_fail count          │
       │                                       │────────────────────────>│              │
       │                                       │  ← Số lần sai hiện tại │              │
       │                                       │                        │               │
       │                                       │  5. Sinh JWT:                          │
       │                                       │     Access Token (15 min)              │
       │                                       │     Refresh Token (7 days)             │
       │                                       │     Payload: {userId, role, deviceId}  │
       │                                       │                                        │
       │                                       │  6. Lưu Refresh Token vào Redis        │
       │                                       │────────────────────────>│              │
       │                                       │  ← SET session:{userId}:{deviceId}    │
       │                                       │                        │               │
       │  7. Response: HTTP 200 OK             │                        │               │
       │  {accessToken, refreshToken, user}    │                        │               │
       │<──────────────────────────────────────│                        │               │
       │                                       │                        │               │
       │  8. Lưu tokens vào                    │                        │               │
       │     EncryptedSharedPreferences        │                        │               │
```

---

### FLOW-03: Chuyển Tiền P2P Transfer (Full Pipeline)

**Đây là luồng phức tạp nhất — thể hiện toàn bộ cơ chế bảo vệ của hệ thống.**

**Tác nhân:** USER + SYSTEM
**Quy tắc áp dụng:** BR-GEN-01 đến BR-GEN-05, BR-SPEC-SEC01, BR-SPEC-TX01 đến BR-SPEC-TX04

```
USER (Android)          SYSTEM (Backend)              REDIS                 PostgreSQL
     │                        │                          │                       │
     │ 1. Nhập SĐT đích,     │                          │                       │
     │    số tiền, mô tả      │                          │                       │
     │                        │                          │                       │
     │ 2. Quét vân tay        │                          │                       │
     │    (BiometricPrompt)   │                          │                       │
     │    → Keystore ký       │                          │                       │
     │    ECDSA(payload)      │                          │                       │
     │                        │                          │                       │
     │ 3. POST /api/v1/       │                          │                       │
     │    transfers            │                          │                       │
     │    Header:              │                          │                       │
     │    Idempotency-Key: K1  │                          │                       │
     │    X-Device-Id: D1      │                          │                       │
     │    Authorization: JWT   │                          │                       │
     │──────────────────────>│                          │                       │
     │                        │                          │                       │
     │                        │ 4. JWT Filter:            │                       │
     │                        │    Verify token           │                       │
     │                        │    Check deviceId         │                       │
     │                        │                          │                       │
     │                        │ 5. IDEMPOTENCY CHECK     │                       │
     │                        │────────────────────────>│                       │
     │                        │ GET idemp:{userId}:{K1}  │                       │
     │                        │                          │                       │
     │                        │ ← Chưa tồn tại          │                       │
     │                        │────────────────────────>│                       │
     │                        │ SET idemp:{K1}=PROCESSING│                       │
     │                        │ (TTL 30s)                │                       │
     │                        │                          │                       │
     │                        │ 6. VALIDATE PIN          │                       │
     │                        │────────────────────────>│                       │
     │                        │ GET pin_fail:{userId}    │                       │
     │                        │ ← Chưa vượt ngưỡng      │                       │
     │                        │                          │                       │
     │                        │ 7. VALIDATE BUSINESS     │                       │
     │                        │    - sourceId != destId   │                       │
     │                        │    - amount >= 10.000     │                       │
     │                        │    - amount <= 5.000.000  │                       │
     │                        │    - daily total check    │                       │
     │                        │                          │                       │
     │                        │ 8. LOCK ORDERING          │                       │
     │                        │    firstId = min(A, B)    │                       │
     │                        │    secondId = max(A, B)   │                       │
     │                        │                          │                       │
     │                        │ 9. ACQUIRE REDIS LOCK    │                       │
     │                        │────────────────────────>│                       │
     │                        │ MultiLock.tryLock(3s)     │                       │
     │                        │ wallet:lock:{firstId}     │                       │
     │                        │ wallet:lock:{secondId}    │                       │
     │                        │                          │                       │
     │                        │ 10. @Transactional BEGIN │                       │
     │                        │                          │                       │
     │                        │ 11. DB ROW LOCK          │                       │
     │                        │──────────────────────────────────────────────>│
     │                        │ SELECT ... FOR UPDATE     │                       │
     │                        │ WHERE id = firstId        │                       │
     │                        │ SELECT ... FOR UPDATE     │                       │
     │                        │ WHERE id = secondId       │                       │
     │                        │                          │                       │
     │                        │ 12. CHECK BALANCE        │                       │
     │                        │ sourceWallet.balance      │                       │
     │                        │   >= amount + fee?        │                       │
     │                        │                          │                       │
     │                        │   [NẾU KHÔNG ĐỦ SỐ DƯ]  │                       │
     │                        │   → ROLLBACK              │                       │
     │                        │   → Release Lock          │                       │
     │                        │   → HTTP 422              │                       │
     │                        │   INSUFFICIENT_FUNDS      │                       │
     │                        │                          │                       │
     │                        │   [NẾU ĐỦ SỐ DƯ]        │                       │
     │                        │                          │                       │
     │                        │ 13. UPDATE BALANCES      │                       │
     │                        │──────────────────────────────────────────────>│
     │                        │ UPDATE wallets            │                       │
     │                        │   SET balance = balance   │                       │
     │                        │     - (amount+fee)        │                       │
     │                        │   WHERE id = sourceId     │                       │
     │                        │                          │                       │
     │                        │ UPDATE wallets            │                       │
     │                        │   SET balance = balance   │                       │
     │                        │     + amount              │                       │
     │                        │   WHERE id = destId       │                       │
     │                        │                          │                       │
     │                        │ 14. INSERT TRANSACTION   │                       │
     │                        │──────────────────────────────────────────────>│
     │                        │ INSERT transactions       │                       │
     │                        │   (status='SUCCESS')      │                       │
     │                        │                          │                       │
     │                        │ 15. INSERT LEDGER (Kế toán kép)                  │
     │                        │──────────────────────────────────────────────>│
     │                        │ INSERT ledger_entries     │                       │
     │                        │   (DEBIT, sourceId,       │                       │
     │                        │    balance_after)         │                       │
     │                        │ INSERT ledger_entries     │                       │
     │                        │   (CREDIT, destId,        │                       │
     │                        │    balance_after)         │                       │
     │                        │                          │                       │
     │                        │ 16. @Transactional COMMIT│                       │
     │                        │──────────────────────────────────────────────>│
     │                        │ ← COMMIT OK              │                       │
     │                        │                          │                       │
     │                        │ 17. RELEASE REDIS LOCK   │                       │
     │                        │────────────────────────>│                       │
     │                        │ MultiLock.unlock()        │                       │
     │                        │                          │                       │
     │                        │ 18. UPDATE IDEMPOTENCY   │                       │
     │                        │────────────────────────>│                       │
     │                        │ SET idemp:{K1}=COMPLETED  │                       │
     │                        │ (TTL 24h, kèm response)  │                       │
     │                        │                          │                       │
     │ 19. HTTP 200 OK        │                          │                       │
     │ {transactionId,         │                          │                       │
     │  status: SUCCESS,       │                          │                       │
     │  amount, fee, timestamp}│                          │                       │
     │<──────────────────────│                          │                       │
```

---

### FLOW-06: Tạo Mã VietQR EMVCo

**Tác nhân:** USER
**Quy tắc áp dụng:** BR-SPEC-QR01

```
USER (Android)                SYSTEM (Backend)                     DATABASE
     │                              │                                  │
     │ 1. Chọn "Nhận tiền"          │                                  │
     │    Nhập số tiền (tuỳ chọn)   │                                  │
     │    Nhập mô tả (tuỳ chọn)    │                                  │
     │                              │                                  │
     │ 2. POST /api/v1/qr-codes     │                                  │
     │    {amount, description}      │                                  │
     │──────────────────────────────>│                                  │
     │                              │ 3. Build TLV Payload:            │
     │                              │    Tag 00 = "01"                 │
     │                              │    Tag 01 = "12" (Dynamic)       │
     │                              │    Tag 38 = Nested:              │
     │                              │      Sub 00 = "A000000727"       │
     │                              │      Sub 01 = Nested:            │
     │                              │        SubSub 00 = BIN (6 số)    │
     │                              │        SubSub 01 = walletId      │
     │                              │      Sub 02 = "QRIBFTTA"         │
     │                              │    Tag 53 = "704" (VND)          │
     │                              │    Tag 54 = amount               │
     │                              │    Tag 62 = description          │
     │                              │    Tag 63 = CRC16-CCITT          │
     │                              │                                  │
     │                              │ 4. INSERT qr_codes               │
     │                              │──────────────────────────────────>│
     │                              │ (payload, expiry=NOW+15min)      │
     │                              │                                  │
     │ 5. HTTP 201 Created          │                                  │
     │ {qrId, payload,              │                                  │
     │  expiryTime}                 │                                  │
     │<──────────────────────────────│                                  │
     │                              │                                  │
     │ 6. Hiển thị QR trên màn hình │                                  │
     │    (Sử dụng ZXing/QRGen      │                                  │
     │     encode payload → Bitmap) │                                  │
```

---

### FLOW-07: Quét & Thanh Toán QR

**Tác nhân:** USER (Người quét) + USER (Người nhận) + SYSTEM

```
USER A (Quét QR)         SYSTEM (Backend)              REDIS           PostgreSQL
     │                        │                          │                  │
     │ 1. Mở Camera (CameraX) │                          │                  │
     │    ML Kit quét QR       │                          │                  │
     │    Nhận chuỗi TLV       │                          │                  │
     │                        │                          │                  │
     │ 2. Validate CRC16      │                          │                  │
     │    trên client (local)  │                          │                  │
     │    [BR-SPEC-QR03]       │                          │                  │
     │                        │                          │                  │
     │ 3. Parse TLV:          │                          │                  │
     │    Trích xuất:          │                          │                  │
     │    - walletId (Tag 38)  │                          │                  │
     │    - amount (Tag 54)    │                          │                  │
     │    - description (62)   │                          │                  │
     │                        │                          │                  │
     │ 4. POST /api/v1/        │                          │                  │
     │    qr-codes/pay          │                          │                  │
     │    {qrPayload, pin}      │                          │                  │
     │──────────────────────>│                          │                  │
     │                        │ 5. Parse & Validate QR   │                  │
     │                        │    trên server (lần 2)    │                  │
     │                        │                          │                  │
     │                        │ 6. Check QR status       │                  │
     │                        │──────────────────────────────────────────>│
     │                        │ ← is_used? expiry_time?  │                  │
     │                        │                          │                  │
     │                        │ 7. [Nếu hợp lệ]         │                  │
     │                        │    Thực thi FLOW-03       │                  │
     │                        │    (P2P Transfer Pipeline)│                  │
     │                        │    sourceWallet = User A  │                  │
     │                        │    destWallet = QR owner  │                  │
     │                        │                          │                  │
     │                        │ 8. Đánh dấu QR đã dùng  │                  │
     │                        │──────────────────────────────────────────>│
     │                        │ UPDATE qr_codes           │                  │
     │                        │   SET is_used = TRUE,     │                  │
     │                        │   transaction_id = txId   │                  │
     │                        │                          │                  │
     │ 9. HTTP 200 OK         │                          │                  │
     │ {transactionId, status} │                          │                  │
     │<──────────────────────│                          │                  │
```

---

### FLOW-08: Admin Khóa Ví Khẩn Cấp

**Tác nhân:** ADMIN + SYSTEM
**Quy tắc áp dụng:** BR-SPEC-ADM01, BR-SPEC-ADM02

```
ADMIN (Next.js)             SYSTEM (Backend)              REDIS           PostgreSQL
     │                            │                          │                │
     │ 1. Chọn User → Bấm        │                          │                │
     │    "Khóa ví khẩn cấp"     │                          │                │
     │                            │                          │                │
     │ 2. POST /api/v1/admin/     │                          │                │
     │    wallets/{id}/freeze      │                          │                │
     │    {reason: "Nghi gian lận"}│                          │                │
     │──────────────────────────>│                          │                │
     │                            │ 3. Đẩy vào Blacklist    │                │
     │                            │    (có hiệu lực tức thì)│                │
     │                            │────────────────────────>│                │
     │                            │ SADD wallet:blacklist    │                │
     │                            │   {walletId}             │                │
     │                            │                          │                │
     │                            │ 4. Cập nhật DB          │                │
     │                            │──────────────────────────────────────>│
     │                            │ UPDATE wallets           │                │
     │                            │   SET status='FROZEN'    │                │
     │                            │                          │                │
     │ 5. HTTP 200 OK             │                          │                │
     │ {walletId, newStatus}      │                          │                │
     │<──────────────────────────│                          │                │
```

---

## III. STATE MACHINE — VÒNG ĐỜI GIAO DỊCH

```
                    ┌───────────────────────────┐
                    │        PENDING             │
                    │  (Request vừa được nhận,   │
                    │   chưa validate xong)      │
                    └─────────────┬─────────────┘
                                  │
                    ┌─────────────▼─────────────┐
                    │       PROCESSING           │
                    │  (Đã acquire lock,          │
                    │   đang thực thi trong TX)   │
                    └──────┬──────────────┬──────┘
                           │              │
              Thành công   │              │  Thất bại
                           ▼              ▼
              ┌────────────────┐  ┌────────────────┐
              │    SUCCESS     │  │     FAILED     │
              │  (DB Committed,│  │  (Rollback,    │
              │   tiền đã     │  │   trả lỗi)    │
              │   biến động)  │  │               │
              └────────────────┘  └────────────────┘
                       │
                       │  Admin hoàn tiền / Hệ thống phát hiện lỗi
                       ▼
              ┌────────────────┐
              │    REVERSED    │
              │  (Giao dịch đảo│
              │   ngược, sinh  │
              │   TX đối ứng)  │
              └────────────────┘
```
