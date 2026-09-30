# 04 - HỆ THỐNG RÀNG BUỘC QUY TẮC NGHIỆP VỤ (BUSINESS RULES)

---

## I. PHƯƠNG PHÁP LUẬN XÂY DỰNG RULE

### 1.1. Quy trình 5 bước tạo Business Rule mới

Mỗi khi phát sinh yêu cầu nghiệp vụ mới hoặc phát hiện lỗ hổng logic, tuân theo quy trình sau:

```
Bước 1: IDENTIFY (Nhận diện)
   │  "Vấn đề/Yêu cầu nghiệp vụ này là gì?"
   │  "Nó thuộc module nào? (AUTH, TX, QR, ADM...)"
   ▼
Bước 2: CLASSIFY (Phân loại)
   │  Đây là Quy tắc Chung (BR-GEN) hay Quy tắc Riêng (BR-SPEC)?
   │  BR-GEN: Áp dụng xuyên suốt toàn bộ hệ thống, mọi module phải tuân thủ.
   │  BR-SPEC: Chỉ áp dụng cho 1 module/chức năng cụ thể.
   ▼
Bước 3: DEFINE (Định nghĩa)
   │  Viết rule theo cấu trúc chuẩn (xem mục 1.2 bên dưới).
   │  Xác định rõ: Điều kiện kích hoạt → Hành vi mong đợi → Hành vi vi phạm.
   ▼
Bước 4: IMPLEMENT (Triển khai)
   │  Xác định rule được thực thi ở tầng nào:
   │  • Database Layer (CHECK constraint, UNIQUE, FK)
   │  • Application Layer (Service validation, Filter)
   │  • Cache Layer (Redis TTL, Rate Limit)
   │  • Client Layer (Input validation trước khi gửi API)
   ▼
Bước 5: VERIFY (Kiểm chứng)
      Viết ít nhất 1 test case (Unit/Integration) kiểm chứng rule hoạt động đúng.
      Viết ít nhất 1 test case kiểm chứng hệ thống từ chối đúng khi rule bị vi phạm.
```

### 1.2. Cấu trúc chuẩn (Template) khi viết Rule

```markdown
### BR-<LOẠI>-<SỐ>: <Tên Quy Tắc>
- **Mã liên quan:** <Feature Code liên quan>
- **Mô tả:** <Mô tả ngắn gọn rule làm gì>
- **Điều kiện:** <Khi nào rule này được kích hoạt/kiểm tra>
- **Hành vi đúng:** <Hệ thống phải làm gì khi thỏa mãn>
- **Hành vi vi phạm:** <Hệ thống phải làm gì khi vi phạm — error code, HTTP status>
- **Tầng thực thi:** <DB / Service / Filter / Client>
- **Test case bắt buộc:** <Mô tả test case kiểm chứng>
```

---

## II. QUY TẮC CHUNG — KIẾN TRÚC & AN TOÀN (BR-GEN)

Các quy tắc này áp dụng **xuyên suốt toàn bộ hệ thống**, mọi module và mọi tầng phải tuân thủ.

---

### BR-GEN-01: Bảo toàn số dư không âm (Non-Negative Balance Guarantee)
- **Mã liên quan:** WAL-01, TX-01, TX-02, TX-03, QR-03
- **Mô tả:** Số dư ví không bao giờ được phép xuống dưới 0 VNĐ, bất kể hoàn cảnh.
- **Điều kiện:** Mỗi khi có thao tác UPDATE hoặc INSERT ảnh hưởng đến cột `wallets.balance`.
- **Hành vi đúng:** Giao dịch chỉ thành công khi `balance - (amount + fee) >= 0`.
- **Hành vi vi phạm:** PostgreSQL ném `CheckConstraintViolationException` → Service catch → Trả HTTP `422` với error code `INSUFFICIENT_FUNDS`.
- **Tầng thực thi:**
  - **DB:** `CONSTRAINT chk_wallet_balance_non_negative CHECK (balance >= 0)` — Phòng tuyến cuối cùng.
  - **Service:** Kiểm tra `if (sourceWallet.getBalance() < totalAmount)` TRƯỚC khi chạy UPDATE — Phòng tuyến đầu tiên.
- **Test case:** Gửi 500 request rút tiền đồng thời, số dư cuối bắt buộc >= 0.

---

### BR-GEN-02: Bảo vệ Idempotency 2 lớp (Dual-Layer Idempotency)
- **Mã liên quan:** TX-01, TX-02, TX-03, QR-03
- **Mô tả:** Mỗi giao dịch tài chính phải gắn liền với 1 Idempotency-Key duy nhất. Gửi lại request với cùng Key sẽ trả kết quả cũ, không thực thi lại.
- **Điều kiện:** Mọi request ghi (POST) liên quan đến biến động số dư.
- **Hành vi đúng:**
  - Lớp 1 (Redis): Kiểm tra `idemp:{userId}:{key}`. Nếu `COMPLETED` → Trả kết quả cũ (HTTP 200). Nếu `PROCESSING` → Trả HTTP 409.
  - Lớp 2 (DB): Ràng buộc `UNIQUE(idempotency_key)` trong bảng `transactions`.
- **Hành vi vi phạm — Payload Tampering:** Nếu trùng Key nhưng `SHA-256(body)` khác với `request_hash` đã lưu → Trả HTTP `422` với error code `IDEMPOTENCY_PAYLOAD_MISMATCH`.
- **Tầng thực thi:** Redis Filter (Lớp 1) + Database UNIQUE Constraint (Lớp 2).
- **Test case:** Gửi cùng Key, payload khác → Nhận 422. Gửi cùng Key, cùng payload → Nhận 200 với kết quả cũ.

---

### BR-GEN-03: Thứ tự khóa chống Deadlock (Lock Ordering)
- **Mã liên quan:** TX-01, QR-03
- **Mô tả:** Khi giao dịch liên quan đến ≥ 2 ví, hệ thống bắt buộc phải sắp xếp và lock theo thứ tự `walletId` tăng dần (UUID `compareTo`).
- **Điều kiện:** Mọi giao dịch có cả `source_wallet_id` và `dest_wallet_id`.
- **Hành vi đúng:** `firstId = min(sourceId, destId)`, `secondId = max(sourceId, destId)`. Lock `firstId` trước, `secondId` sau.
- **Hành vi vi phạm:** Nếu lock ngẫu nhiên → Deadlock xảy ra khi 2 user chuyển tiền chéo nhau.
- **Tầng thực thi:** Service Layer (`WalletTransferService`).
- **Test case:** 200 threads A→B đồng thời với 200 threads B→A. Kết quả: 0 lỗi Deadlock PostgreSQL (40P01).

---

### BR-GEN-04: Phạm vi Lock ngoài Transaction (Lock-Outside-TX)
- **Mã liên quan:** TX-01, TX-03, QR-03
- **Mô tả:** Redis Distributed Lock phải được acquire TRƯỚC khi mở `@Transactional` và release SAU KHI DB đã Commit/Rollback.
- **Điều kiện:** Mọi giao dịch sử dụng Redisson Lock.
- **Hành vi đúng:**
  ```
  1. Acquire Redis Lock
  2. → @Transactional begins
  3. → Execute business logic
  4. → @Transactional commits
  5. Release Redis Lock
  ```
- **Hành vi vi phạm:** Nếu Release Lock trước Commit → Race Condition: luồng khác đọc được dữ liệu cũ từ DB (chưa commit).
- **Tầng thực thi:** Service Layer — tách thành 2 class: `TransferService` (giữ lock, gọi inner service) và `TransferInternalService` (chứa `@Transactional`).

---

### BR-GEN-05: Kế toán kép bắt buộc (Mandatory Double-Entry)
- **Mã liên quan:** TX-01, TX-02, TX-03, QR-03, ADM-03
- **Mô tả:** Mỗi giao dịch thành công bắt buộc phải sinh ra ít nhất 2 bút toán trong bảng `ledger_entries`: 1 DEBIT (trừ) và 1 CREDIT (cộng).
- **Hành vi đúng:** Tổng đại số biến động (ΣCREDIT - ΣDEBIT) trong toàn hệ thống = 0 (trừ Top-up/Withdraw vì liên quan bên thứ ba).
- **Tầng thực thi:** Service Layer — trong cùng `@Transactional` với UPDATE balance.
- **Test case (ADM-03 Đối soát):**
  ```sql
  SELECT
    SUM(CASE WHEN entry_type = 'CREDIT' THEN amount ELSE 0 END) -
    SUM(CASE WHEN entry_type = 'DEBIT' THEN amount ELSE 0 END) AS net_balance
  FROM ledger_entries
  WHERE transaction_id IN (SELECT id FROM transactions WHERE type = 'P2P_TRANSFER');
  -- KẾT QUẢ BẮT BUỘC: 0
  ```

---

### BR-GEN-06: Không trả Entity ra ngoài Controller (DTO Boundary)
- **Mã liên quan:** Toàn bộ
- **Mô tả:** Tầng Controller không bao giờ trả trực tiếp JPA Entity. Luôn chuyển đổi sang DTO/Response object.
- **Lý do:** Ngăn chặn lộ thông tin nhạy cảm (`password_hash`, `pin_hash`), tránh `LazyInitializationException`, và kiểm soát response shape.
- **Tầng thực thi:** Service/Mapper Layer.

---

### BR-GEN-07: Mọi Timestamp đều dùng UTC (Timezone Normalization)
- **Mã liên quan:** Toàn bộ
- **Mô tả:** Toàn bộ cột thời gian trong DB dùng kiểu `TIMESTAMPTZ`. Backend xử lý dưới dạng `Instant` (UTC). Client tự chuyển đổi sang timezone local để hiển thị.
- **Tầng thực thi:** DB (TIMESTAMPTZ) + JVM (`-Duser.timezone=UTC`) + Client (format hiển thị).

---

## III. QUY TẮC RIÊNG THEO MODULE (BR-SPEC)

---

### Module AUTH (Xác thực & Đăng ký)

#### BR-SPEC-AUTH01: Định dạng số điện thoại Việt Nam
- **Mã liên quan:** AUTH-01
- **Mô tả:** Số điện thoại phải đúng định dạng Việt Nam: bắt đầu bằng `0`, theo sau là 9 chữ số (tổng 10 số). Regex: `^0[3-9]\d{8}$`.
- **Hành vi vi phạm:** HTTP 400 `INVALID_PHONE_FORMAT`.
- **Tầng thực thi:** Client (pre-validation) + Controller (server-side validation `@Pattern`).

#### BR-SPEC-AUTH02: Mật khẩu tối thiểu 8 ký tự
- **Mã liên quan:** AUTH-01
- **Mô tả:** Mật khẩu tối thiểu 8 ký tự, phải có ít nhất 1 chữ hoa, 1 chữ thường, 1 số.
- **Tầng thực thi:** Client + Controller (`@Pattern`).

#### BR-SPEC-AUTH03: Tự động tạo ví khi đăng ký thành công
- **Mã liên quan:** AUTH-01, WAL-01
- **Mô tả:** Khi `users` INSERT thành công, hệ thống tự động INSERT 1 record vào `wallets` với `balance = 0`, `status = 'ACTIVE'`, `currency = 'VND'`. Thao tác này nằm trong cùng 1 `@Transactional`.
- **Tầng thực thi:** Service Layer (`AuthService.register()`).

---

### Module SEC (Bảo mật)

#### BR-SPEC-SEC01: Chống Brute-force PIN (Rate Limiting)
- **Mã liên quan:** SEC-02, SEC-03
- **Mô tả:** Đếm số lần nhập sai PIN liên tiếp trong Redis. Vượt quá ngưỡng → tạm khóa.
- **Hành vi chi tiết:**
  - Nhập sai 1–2 lần: Cho phép thử tiếp. Trả cảnh báo `Bạn còn {n} lần thử`.
  - Nhập sai 3 lần: Tạm khóa giao dịch 5 phút.
  - Nhập sai 5 lần: Khóa chức năng thanh toán 24h. Yêu cầu mở khóa qua Admin/OTP.
- **Redis Key:** `pin_fail:{userId}` → Value: số lần sai (TTL: 15 phút, reset khi nhập đúng).
- **Hành vi vi phạm:** HTTP 423 `ACCOUNT_LOCKED` kèm `retry_after_seconds`.
- **Tầng thực thi:** Service Layer + Redis.

#### BR-SPEC-SEC02: Xác thực thiết bị (Device Binding)
- **Mã liên quan:** SEC-01, AUTH-02
- **Mô tả:** Token JWT chỉ hợp lệ khi `X-Device-Id` trong Header trùng với `device_id` đã đăng ký trong bảng `device_keys`.
- **Hành vi vi phạm:** HTTP 401 `DEVICE_NOT_REGISTERED`.

#### BR-SPEC-SEC03: PIN không được truyền dạng Plain-text trong Production
- **Mã liên quan:** SEC-02
- **Mô tả:** Client phải hash PIN kèm Nonce/Timestamp trước khi gửi lên server. Server chỉ nhận giá trị đã hash.
- **Định dạng gửi lên:** `SHA-256(pin + nonce + timestamp)`.
- **Tầng thực thi:** Client Layer (Android) + Server Filter (verify).

---

### Module TX (Giao dịch)

#### BR-SPEC-TX01: Hạn mức giao dịch
- **Mã liên quan:** TX-01, TX-02, TX-03
- **Mô tả:** Mỗi giao dịch và tổng giao dịch trong ngày phải nằm trong giới hạn cho phép.
- **Giá trị mặc định:** (Đọc từ bảng `system_configs`)
  - Tối thiểu/lần: `10.000 VNĐ`
  - Tối đa/lần: `5.000.000 VNĐ`
  - Tối đa/ngày: `20.000.000 VNĐ`
  - Số dư tối đa trong ví: `100.000.000 VNĐ`
- **Hành vi vi phạm:**
  - Dưới mức tối thiểu: HTTP 422 `BELOW_MINIMUM_AMOUNT`
  - Vượt mức/lần: HTTP 422 `EXCEEDS_PER_TRANSACTION_LIMIT`
  - Vượt mức/ngày: HTTP 422 `EXCEEDS_DAILY_LIMIT`
  - Ví nhận vượt trần: HTTP 422 `DEST_WALLET_EXCEEDS_MAX_BALANCE`
- **Tầng thực thi:** Service Layer (query tổng giao dịch trong ngày từ DB + so sánh).

#### BR-SPEC-TX02: Không thể tự chuyển tiền cho chính mình
- **Mã liên quan:** TX-01
- **Mô tả:** `source_wallet_id` phải khác `dest_wallet_id`.
- **Hành vi vi phạm:** HTTP 422 `SELF_TRANSFER_NOT_ALLOWED`.
- **Tầng thực thi:** Service Layer (validation đầu).

#### BR-SPEC-TX03: Biểu phí giao dịch
- **Mã liên quan:** TX-01, TX-03
- **Mô tả:** (Đọc từ bảng `system_configs`)
  - P2P Transfer: `0 VNĐ` (miễn phí).
  - Top-up: `0 VNĐ` (miễn phí).
  - Withdraw: `1.100 VNĐ + 0.1%` giá trị giao dịch.
- **Tầng thực thi:** Service Layer (`FeeCalculationService`).

#### BR-SPEC-TX04: Ví bị khóa (FROZEN) không thể giao dịch
- **Mã liên quan:** TX-01, TX-02, TX-03, QR-03
- **Mô tả:** Nếu `wallets.status != 'ACTIVE'`, mọi giao dịch đều bị từ chối.
- **Hành vi vi phạm:** HTTP 403 `WALLET_FROZEN`.
- **Tầng thực thi:** Service Layer (check status trước khi lock).

---

### Module QR (Mã VietQR)

#### BR-SPEC-QR01: Thời gian sống QR động
- **Mã liên quan:** QR-01
- **Mô tả:** Mã QR động (Dynamic, `Tag 01 = 12`) hết hạn sau `15 phút` (đọc từ `system_configs`). QR tĩnh (Static, `Tag 01 = 11`) không hết hạn.
- **Tầng thực thi:** Service Layer + SYSTEM Worker thu hồi QR hết hạn.

#### BR-SPEC-QR02: QR động chỉ dùng được 1 lần (Single-Use)
- **Mã liên quan:** QR-03
- **Mô tả:** Khi QR động được thanh toán thành công, cờ `is_used = TRUE` và `transaction_id` được gắn liền. Quét lại mã đã thanh toán → Từ chối.
- **Hành vi vi phạm:** HTTP 410 `QR_ALREADY_USED`.
- **Tầng thực thi:** Service Layer.

#### BR-SPEC-QR03: Validate CRC16 khi quét QR
- **Mã liên quan:** QR-02
- **Mô tả:** Sau khi Camera quét được chuỗi QR, client phải tính lại checksum CRC16-CCITT và so khớp với 4 ký tự cuối (Tag 63). Nếu không khớp → Từ chối.
- **Hành vi vi phạm:** Hiển thị dialog lỗi `Mã QR không hợp lệ hoặc đã bị thay đổi`.
- **Tầng thực thi:** Client Layer (Android — validate trước khi gọi API).

---

### Module ADM (Quản trị)

#### BR-SPEC-ADM01: Phân quyền theo Role
- **Mã liên quan:** ADM-01, ADM-02, ADM-03, ADM-04
- **Mô tả:** Chỉ các role được phép mới truy cập được endpoint Admin.
  - `ADMIN`: Xem Dashboard, tra cứu User, đối soát.
  - `SUPER_ADMIN`: Toàn bộ quyền ADMIN + Cấu hình hạn mức/biểu phí + Phân quyền Admin.
  - `OWNER`: Toàn bộ quyền SUPER_ADMIN + Xem báo cáo tài chính tổng hợp.
- **Hành vi vi phạm:** HTTP 403 `ACCESS_DENIED`.
- **Tầng thực thi:** Spring Security (`@PreAuthorize`).

#### BR-SPEC-ADM02: Khóa ví khẩn cấp (Emergency Freeze)
- **Mã liên quan:** ADM-02
- **Mô tả:** Admin có thể khóa ví (set `status = 'FROZEN'`) ngay lập tức. Hiệu lực phải áp dụng tức thì — hệ thống phải đẩy `walletId` vào Redis Blacklist để chặn giao dịch mới trước khi DB cập nhật xong.
- **Tầng thực thi:** Service Layer + Redis Set (`wallet:blacklist`).

---

## IV. BẢNG TỔNG HỢP ERROR CODES

| Error Code | HTTP Status | Mô Tả | Rule Liên Quan |
| :--- | :---: | :--- | :--- |
| `INVALID_PHONE_FORMAT` | 400 | SĐT không đúng định dạng VN | BR-SPEC-AUTH01 |
| `WEAK_PASSWORD` | 400 | Mật khẩu không đủ mạnh | BR-SPEC-AUTH02 |
| `PHONE_ALREADY_EXISTS` | 409 | SĐT đã được đăng ký | BR-SPEC-AUTH01 |
| `INVALID_CREDENTIALS` | 401 | Sai SĐT hoặc mật khẩu | AUTH-02 |
| `DEVICE_NOT_REGISTERED` | 401 | Thiết bị chưa đăng ký | BR-SPEC-SEC02 |
| `INVALID_PIN` | 401 | Mã PIN không chính xác | SEC-02 |
| `ACCOUNT_LOCKED` | 423 | Tài khoản bị tạm khóa (nhập sai PIN quá nhiều) | BR-SPEC-SEC01 |
| `INSUFFICIENT_FUNDS` | 422 | Số dư không đủ | BR-GEN-01 |
| `BELOW_MINIMUM_AMOUNT` | 422 | Số tiền dưới mức tối thiểu | BR-SPEC-TX01 |
| `EXCEEDS_PER_TRANSACTION_LIMIT` | 422 | Vượt hạn mức/lần | BR-SPEC-TX01 |
| `EXCEEDS_DAILY_LIMIT` | 422 | Vượt hạn mức/ngày | BR-SPEC-TX01 |
| `DEST_WALLET_EXCEEDS_MAX_BALANCE` | 422 | Ví đích vượt trần số dư | BR-SPEC-TX01 |
| `SELF_TRANSFER_NOT_ALLOWED` | 422 | Không thể tự chuyển cho mình | BR-SPEC-TX02 |
| `WALLET_FROZEN` | 403 | Ví đã bị khóa | BR-SPEC-TX04 |
| `WALLET_NOT_FOUND` | 404 | Không tìm thấy ví | WAL-01 |
| `IDEMPOTENCY_PAYLOAD_MISMATCH` | 422 | Key trùng nhưng payload khác | BR-GEN-02 |
| `DUPLICATE_REQUEST` | 409 | Request đang được xử lý | BR-GEN-02 |
| `CONCURRENCY_CONFLICT` | 409 | Không lấy được lock (hệ thống bận) | BR-GEN-03 |
| `QR_EXPIRED` | 410 | Mã QR đã hết hạn | BR-SPEC-QR01 |
| `QR_ALREADY_USED` | 410 | Mã QR đã được sử dụng | BR-SPEC-QR02 |
| `QR_INVALID_CHECKSUM` | 422 | Checksum CRC16 không khớp | BR-SPEC-QR03 |
| `ACCESS_DENIED` | 403 | Không đủ quyền truy cập | BR-SPEC-ADM01 |
