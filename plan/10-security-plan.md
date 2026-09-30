# 10 - KẾ HOẠCH BẢO MẬT TOÀN DIỆN (SECURITY PLAN)

---

## I. MÔ HÌNH BẢO MẬT NHIỀU LỚP (DEFENSE IN DEPTH)

```
┌──────────────────────────────────────────────────────────┐
│  LỚP 1: TRANSPORT — HTTPS/TLS 1.3                        │
│  Mã hóa toàn bộ dữ liệu truyền tải trên đường truyền    │
├──────────────────────────────────────────────────────────┤
│  LỚP 2: AUTHENTICATION — JWT + Device Binding            │
│  Xác thực danh tính người dùng và thiết bị                │
├──────────────────────────────────────────────────────────┤
│  LỚP 3: AUTHORIZATION — Role-Based Access (RBAC)         │
│  Phân quyền theo vai trò: USER, ADMIN, SUPER_ADMIN, OWNER│
├──────────────────────────────────────────────────────────┤
│  LỚP 4: TRANSACTION SIGNING — ECDSA + Biometric          │
│  Ký số giao dịch bằng Private Key phần cứng + vân tay    │
├──────────────────────────────────────────────────────────┤
│  LỚP 5: RATE LIMITING — Redis Counter                    │
│  Chống Brute-force PIN, chống DDoS endpoint nhạy cảm     │
├──────────────────────────────────────────────────────────┤
│  LỚP 6: DATA INTEGRITY — DB Constraints + Idempotency    │
│  Ràng buộc CHECK balance>=0, UNIQUE idempotency_key       │
└──────────────────────────────────────────────────────────┘
```

---

## II. XÁC THỰC (AUTHENTICATION)

### 2.1. JWT Token Architecture

| Loại Token | Lưu trữ (Android) | Lưu trữ (Web Admin) | Thời hạn | Payload |
| :--- | :--- | :--- | :--- | :--- |
| **Access Token** | EncryptedSharedPrefs | In-Memory (JS variable) | 15 phút | `{userId, role, deviceId}` |
| **Refresh Token** | EncryptedSharedPrefs | HttpOnly Secure Cookie | 7 ngày | `{userId, deviceId, tokenFamily}` |

### 2.2. Luồng Refresh Token (Rotation)
```
1. Access Token hết hạn → Client gửi Refresh Token
2. Server kiểm tra Refresh Token hợp lệ
3. Server sinh Access Token MỚI + Refresh Token MỚI
4. Server vô hiệu hóa Refresh Token CŨ (one-time use)
5. Nếu phát hiện Refresh Token cũ đã bị dùng lại → Thu hồi TOÀN BỘ token
   (dấu hiệu token bị đánh cắp)
```

### 2.3. Device Binding
- Mỗi JWT gắn liền với `deviceId`.
- Khi đăng nhập trên thiết bị mới → Refresh Token trên thiết bị cũ bị thu hồi.
- Header `X-Device-Id` bắt buộc → So khớp với `deviceId` trong JWT payload.

---

## III. PHÂN QUYỀN (AUTHORIZATION)

### Spring Security Config:
```java
@Bean
SecurityFilterChain securityFilterChain(HttpSecurity http) {
    http
        .csrf(csrf -> csrf.disable())
        .sessionManagement(sm -> sm.sessionCreationPolicy(STATELESS))
        .authorizeHttpRequests(auth -> auth
            // Public endpoints
            .requestMatchers("/api/v1/auth/**").permitAll()
            .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

            // USER endpoints
            .requestMatchers("/api/v1/wallets/**").hasAnyRole("USER", "ADMIN")
            .requestMatchers("/api/v1/transfers/**").hasRole("USER")
            .requestMatchers("/api/v1/qr-codes/**").hasRole("USER")
            .requestMatchers("/api/v1/devices/**").hasRole("USER")

            // ADMIN endpoints
            .requestMatchers("/api/v1/admin/dashboard").hasAnyRole("ADMIN", "SUPER_ADMIN", "OWNER")
            .requestMatchers("/api/v1/admin/users/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
            .requestMatchers("/api/v1/admin/wallets/*/freeze").hasAnyRole("ADMIN", "SUPER_ADMIN")
            .requestMatchers("/api/v1/admin/reconciliation").hasAnyRole("ADMIN", "SUPER_ADMIN", "OWNER")
            .requestMatchers("/api/v1/admin/configs/**").hasAnyRole("SUPER_ADMIN", "OWNER")

            .anyRequest().authenticated()
        )
        .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
}
```

---

## IV. KÝ SỐ GIAO DỊCH (TRANSACTION SIGNING)

### Luồng ký số ECDSA (Android → Backend):
```
Android App:
  1. Tạo payload: JSON string của request body
  2. BiometricPrompt → Quét vân tay thành công
  3. Android Keystore giải phóng Private Key (ECDSA P-256)
  4. Signature.sign(SHA256(payload)) → byte[]
  5. Base64.encode(signature) → Gửi kèm header/body

Backend:
  1. Lấy Public Key từ bảng device_keys (theo userId + deviceId)
  2. Signature.verify(SHA256(payload), publicKeyBytes, signatureBytes)
  3. Nếu hợp lệ → Tiếp tục xử lý giao dịch
  4. Nếu không hợp lệ → HTTP 401 INVALID_SIGNATURE
```

---

## V. CHỐNG BRUTE-FORCE PIN

### Cơ chế Redis Counter:
```
Redis Key: pin_fail:{userId}
Value: Số lần nhập sai liên tiếp
TTL: 15 phút (reset tự động nếu không sai thêm)

Logic:
  if (pin sai):
    INCR pin_fail:{userId}
    count = GET pin_fail:{userId}

    if (count == 3):
      → Tạm khóa giao dịch 5 phút
      → Response: "Tạm khóa 5 phút. Còn 2 lần thử."

    if (count >= 5):
      → Khóa chức năng thanh toán 24h
      → SET pin_locked:{userId} = true (TTL 24h)
      → Response: HTTP 423 ACCOUNT_LOCKED

  if (pin đúng):
    DEL pin_fail:{userId}
    → Tiếp tục xử lý giao dịch
```

---

## VI. BẢO VỆ DỮ LIỆU NHẠY CẢM

| Dữ liệu | Phương pháp bảo vệ | Nơi lưu |
| :--- | :--- | :--- |
| Mật khẩu | BCrypt (cost=12) | DB `users.password_hash` |
| PIN | BCrypt (cost=12) | DB `users.pin_hash` |
| Access Token | — | Android: EncryptedSharedPrefs / Web: Memory |
| Refresh Token | — | Android: EncryptedSharedPrefs / Web: HttpOnly Cookie |
| Private Key ECDSA | Hardware Keystore (TEE/StrongBox) | Chip bảo mật điện thoại |
| Connection String DB | Biến môi trường | `.env` (không commit Git) |

---

## VII. CHECKLIST BẢO MẬT TRƯỚC KHI DEPLOY

- [ ] Tất cả endpoint nhạy cảm đều yêu cầu `Authorization` header.
- [ ] PIN không bao giờ xuất hiện dạng plaintext trong log server.
- [ ] `X-Content-Type-Options: nosniff` được set trong response header.
- [ ] CORS chỉ cho phép origin cụ thể (không dùng `*`).
- [ ] Rate Limiting đã kích hoạt cho `/auth/login` và `/transfers`.
- [ ] SQL Injection: Sử dụng JPA Parameterized Query — không nối chuỗi SQL.
- [ ] File `.env` và credential không có trong Git history.
