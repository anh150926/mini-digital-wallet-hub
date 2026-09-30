# 13 - QUẢN LÝ PHIÊN & VÒNG ĐỜI TOKEN (SESSION LIFECYCLE)

---

## I. KIẾN TRÚC PHIÊN HOẠT ĐỘNG

### 1.1. State Machine — Phiên người dùng

```
┌──────────┐                               ┌──────────────────┐
│  GUEST   │──── Đăng ký / Đăng nhập ────>│  AUTHENTICATED   │
│ (Chưa    │                               │  (Có JWT Token)  │
│  xác     │<── Token hết hạn / Logout ───│                  │
│  thực)   │                               │  Access Token:   │
└──────────┘                               │  15 phút         │
                                            │  Refresh Token:  │
                                            │  7 ngày           │
                                            └────────┬─────────┘
                                                     │
                                            Bấm Chuyển tiền /
                                            Quét QR / Rút tiền
                                                     │
                                                     ▼
                                            ┌──────────────────┐
                                            │  TX_CHALLENGE    │
                                            │  (Yêu cầu xác   │
                                            │   thực giao dịch)│
                                            └────────┬─────────┘
                                                     │
                                            ┌────────┴────────┐
                                            │                 │
                                            ▼                 ▼
                                   ┌──────────────┐  ┌──────────────┐
                                   │  BIOMETRIC   │  │  PIN_ENTRY   │
                                   │  (Quét vân   │  │  (Nhập PIN   │
                                   │   tay)       │  │   6 số)      │
                                   └──────┬───────┘  └──────┬───────┘
                                          │                 │
                                          ▼                 ▼
                                   ┌──────────────────────────────┐
                                   │  TRANSACTION_SIGNED          │
                                   │  (Payload đã được ký ECDSA   │
                                   │   bằng Private Key phần cứng)│
                                   └──────────────┬───────────────┘
                                                  │
                                                  ▼
                                   ┌──────────────────────────────┐
                                   │  EXECUTING                   │
                                   │  (Gửi request lên Backend,   │
                                   │   chờ kết quả)               │
                                   └──────────────────────────────┘
```

---

## II. VÒNG ĐỜI TOKEN CHI TIẾT

### 2.1. Timeline một phiên đăng nhập điển hình:

```
T=0min     T=15min    T=30min    T=45min    ...    T=7days
  │           │           │           │              │
  ▼           ▼           ▼           ▼              ▼
┌─────┐   ┌─────┐   ┌─────┐   ┌─────┐          ┌─────┐
│Login│   │AT #1│   │AT #2│   │AT #3│          │RT   │
│     │   │hết  │   │hết  │   │hết  │          │hết  │
│Sinh │   │hạn  │   │hạn  │   │hạn  │          │hạn  │
│AT#1 │   │     │   │     │   │     │          │     │
│RT#1 │   │Dùng │   │Dùng │   │Dùng │          │Phải │
│     │   │RT#1 │   │RT#2 │   │RT#3 │          │đăng │
│     │   │sinh │   │sinh │   │sinh │          │nhập │
│     │   │AT#2 │   │AT#3 │   │AT#4 │          │lại  │
│     │   │RT#2 │   │RT#3 │   │RT#4 │          │     │
└─────┘   └─────┘   └─────┘   └─────┘          └─────┘

AT = Access Token (15 phút)
RT = Refresh Token (7 ngày, one-time use, rotation)
```

### 2.2. Redis Session Storage:

```
Redis Keys cho mỗi user session:

session:{userId}:{deviceId}
├── value: {refreshTokenHash, tokenFamily, createdAt}
├── TTL: 604800 (7 ngày)
└── Mục đích: Quản lý Refresh Token + Device Binding

token:blacklist:{jti}
├── value: "revoked"
├── TTL: Bằng thời gian còn lại của token
└── Mục đích: Thu hồi token cụ thể (logout, stolen detection)
```

---

## III. CÁC KỊCH BẢN PHIÊN QUAN TRỌNG

### 3.1. Kịch bản: Access Token hết hạn (Happy Path)
```
1. Client gọi API → Backend trả 401 (token expired)
2. Client tự động gọi POST /api/v1/auth/refresh (gửi kèm refreshToken)
3. Backend:
   a. Verify refreshToken signature
   b. Kiểm tra refreshTokenHash trong Redis session:{userId}:{deviceId}
   c. Kiểm tra tokenFamily khớp
   d. Sinh Access Token MỚI + Refresh Token MỚI
   e. Cập nhật Redis: lưu refreshTokenHash mới, xóa hash cũ
4. Client nhận token mới → Retry request ban đầu
```

### 3.2. Kịch bản: Phát hiện Refresh Token bị đánh cắp
```
Giả sử:
- Hacker đánh cắp Refresh Token RT#2 của User
- User vẫn đang dùng app bình thường

Diễn biến:
1. Hacker dùng RT#2 để refresh → Backend sinh RT#3 (cho Hacker)
2. User dùng RT#2 (cũ) để refresh → Backend phát hiện:
   "RT#2 đã bị sử dụng rồi! Token Family bị compromise!"
3. Backend:
   a. XÓA TOÀN BỘ session:{userId}:{deviceId}
   b. Đưa tất cả token của user vào blacklist
   c. Trả 401 cho cả User và Hacker
4. User bắt buộc phải đăng nhập lại
```

### 3.3. Kịch bản: Đăng nhập trên thiết bị mới
```
1. User đăng nhập trên Device B (đang có session trên Device A)
2. Backend:
   a. Sinh token mới cho Device B
   b. GIỮA NGUYÊN session của Device A (cho phép multi-device)
   c. Nếu muốn single-device: XÓA session Device A (force logout)
3. Device A gọi API → Refresh Token bị từ chối → Phải đăng nhập lại
```

### 3.4. Kịch bản: Logout chủ động
```
1. User bấm Logout trên app
2. Client gọi POST /api/v1/auth/logout (gửi kèm refreshToken)
3. Backend:
   a. Thêm Access Token hiện tại vào blacklist (Redis, TTL = thời gian còn lại)
   b. Xóa session:{userId}:{deviceId} trong Redis
4. Client xóa token khỏi EncryptedSharedPreferences
5. Navigate về màn hình Login
```

---

## IV. XÁC THỰC GIAO DỊCH (TRANSACTION AUTHENTICATION)

### 4.1. Luồng xác thực 2 bước cho mỗi giao dịch tài chính:

```
Bước 1: Xác thực danh tính (WHO)
  → JWT Token hợp lệ + Device ID khớp
  → Đã đăng nhập, đúng người, đúng thiết bị

Bước 2: Xác thực ý chí (INTENT)
  → Quét vân tay (BiometricPrompt) HOẶC Nhập PIN
  → Ký payload bằng Private Key (ECDSA)
  → Chứng minh người dùng CHỦ ĐÍCH muốn thực hiện giao dịch này
```

### 4.2. Quyết định chọn Biometric hay PIN:

| Trường hợp | Phương thức | Lý do |
| :--- | :--- | :--- |
| Thiết bị có vân tay/Face ID | **Biometric (ưu tiên)** | UX tốt nhất, bảo mật cao nhất |
| Biometric thất bại 3 lần | **Fallback → PIN** | Cho phép thử lại bằng PIN |
| Thiết bị không có biometric | **PIN** | Phương thức duy nhất |
| Thiết bị có biometric nhưng chưa đăng ký | **PIN** | Hướng dẫn đăng ký biometric |

---

## V. REDIS KEY SCHEMA TỔNG HỢP

| Redis Key Pattern | Kiểu | TTL | Mục Đích |
| :--- | :--- | :--- | :--- |
| `session:{userId}:{deviceId}` | Hash | 7 ngày | Quản lý Refresh Token |
| `token:blacklist:{jti}` | String | Thời gian còn lại | Thu hồi token |
| `idemp:{userId}:{key}` | Hash | 30s (PROCESSING) / 24h (COMPLETED) | Idempotency |
| `wallet:lock:{walletId}` | Lock | Watchdog auto-renew | Distributed Lock |
| `pin_fail:{userId}` | Counter | 15 phút | Đếm số lần nhập sai PIN |
| `pin_locked:{userId}` | Boolean | 5 phút / 24h | Trạng thái khóa PIN |
| `wallet:blacklist` | Set | Không hết hạn | Danh sách ví bị khóa (Admin) |
