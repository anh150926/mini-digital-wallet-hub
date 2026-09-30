# 02 - KIẾN TRÚC HỆ THỐNG TỔNG THỂ (ARCHITECTURE PLAN)

---

## I. SƠ ĐỒ KIẾN TRÚC TỔNG QUAN

```
┌─────────────────────────────────────────────────────────────────────┐
│                        INTERNET / CLIENT LAYER                       │
│                                                                       │
│   ┌──────────────────┐              ┌──────────────────────────┐     │
│   │  Android App     │              │  Next.js Admin Portal     │     │
│   │  (Java 17/MVVM)  │              │  (TypeScript/React)       │     │
│   │  ─────────────── │              │  ───────────────────────  │     │
│   │  • CameraX/MLKit │              │  • Dashboard (ADM-01)     │     │
│   │  • BiometricAuth │              │  • User Mgmt (ADM-02)     │     │
│   │  • ECDSA Keystore│              │  • Reconciliation (ADM-03)│     │
│   │  • Retrofit 2    │              │  • Settings (ADM-04)      │     │
│   └───────┬──────────┘              └───────────┬────────────────┘    │
│           │ HTTPS (REST API)                    │ HTTPS (REST API)    │
└───────────┼─────────────────────────────────────┼────────────────────┘
            │                                     │
            ▼                                     ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    API GATEWAY / BACKEND LAYER                       │
│                    (Spring Boot 3.x / Java 21)                       │
│                                                                       │
│   ┌────────────────────────────────────────────────────────────┐     │
│   │                    Security Filter Chain                     │    │
│   │  ┌──────────┐  ┌──────────────┐  ┌────────────────────┐   │    │
│   │  │ JWT Auth │→ │ Idempotency  │→ │ Rate Limit / PIN   │   │    │
│   │  │ Filter   │  │ Filter(Redis)│  │ Brute-force Guard  │   │    │
│   │  └──────────┘  └──────────────┘  └────────────────────┘   │    │
│   └────────────────────────────────────────────────────────────┘     │
│                              │                                        │
│   ┌──────────────────────────┼──────────────────────────────────┐    │
│   │              Service / Business Logic Layer                   │   │
│   │                                                               │   │
│   │  ┌──────────┐ ┌──────────────┐ ┌───────────┐ ┌───────────┐ │   │
│   │  │ Auth     │ │ Wallet       │ │Transaction│ │ QR Code   │ │   │
│   │  │ Service  │ │ Service      │ │ Service   │ │ Service   │ │   │
│   │  │(AUTH-01  │ │(WAL-01,02)   │ │(TX-01~03) │ │(QR-01~03) │ │   │
│   │  │ AUTH-02) │ │              │ │           │ │           │ │   │
│   │  └──────────┘ └──────────────┘ └───────────┘ └───────────┘ │   │
│   │                                                               │   │
│   │  ┌──────────────────────────────────────────────────────────┐ │   │
│   │  │             Concurrency Control Layer                     │ │   │
│   │  │  • Redisson MultiLock (Distributed Lock + Watchdog)       │ │   │
│   │  │  • Lock Ordering (UUID compareTo — chống Deadlock)        │ │   │
│   │  │  • Pessimistic Write Lock (SELECT FOR UPDATE fallback)    │ │   │
│   │  └──────────────────────────────────────────────────────────┘ │   │
│   └──────────────────────────────────────────────────────────────┘   │
│                              │                                        │
└──────────────────────────────┼────────────────────────────────────────┘
                               │
            ┌──────────────────┼──────────────────┐
            │                  │                  │
            ▼                  ▼                  ▼
┌────────────────┐  ┌────────────────┐  ┌────────────────────┐
│   PostgreSQL   │  │   Redis 7      │  │   File Storage     │
│   16 (Cloud)   │  │   (Cloud)      │  │   (Optional)       │
│  ────────────  │  │  ────────────  │  │  ────────────────  │
│  • Neon.tech   │  │  • Upstash     │  │  • QR Images       │
│  • Supabase    │  │  • Redis Cloud │  │  • Receipt PDF     │
│  ────────────  │  │  ────────────  │  │                    │
│  Tables:       │  │  Keys:         │  │                    │
│  • users       │  │  • idemp:{key} │  │                    │
│  • wallets     │  │  • wallet:lock │  │                    │
│  • transactions│  │  • pin_fail:{} │  │                    │
│  • ledger_     │  │  • session:{}  │  │                    │
│    entries     │  │  • blacklist:{}│  │                    │
│  • qr_codes    │  │               │  │                    │
└────────────────┘  └────────────────┘  └────────────────────┘
```

---

## II. NGUYÊN TẮC KIẾN TRÚC

### 2.1. Layered Architecture (Kiến trúc phân tầng)

Mỗi module trong Backend tuân theo 4 tầng nghiêm ngặt:

| Tầng | Package | Trách nhiệm | Phụ thuộc |
| :--- | :--- | :--- | :--- |
| **Controller** | `*.controller` | Nhận request, validate input, trả response | → Service |
| **Service** | `*.service` | Chứa toàn bộ business logic, điều phối lock | → Repository |
| **Repository** | `*.repository` | Truy vấn DB (JPA), Pessimistic Lock | → Entity |
| **Entity/Model** | `*.entity`, `*.dto` | Ánh xạ bảng DB, Data Transfer Objects | — |

### 2.2. Quy tắc phụ thuộc (Dependency Rules)
- Controller **KHÔNG ĐƯỢC** gọi trực tiếp Repository — phải đi qua Service.
- Service có thể gọi nhiều Repository trong cùng một `@Transactional`.
- Entity **KHÔNG BAO GIỜ** được trả ra ngoài Controller — luôn chuyển sang DTO/Response.
- Lock Redis (Redisson) được bọc ở tầng Service **bên ngoài** phạm vi `@Transactional`.

### 2.3. Module Communication
- **Android ↔ Backend:** RESTful API qua HTTPS (JSON). Header bắt buộc: `Authorization`, `Idempotency-Key`, `X-Device-Id`.
- **Next.js Admin ↔ Backend:** RESTful API qua HTTPS. Sử dụng `HttpOnly Cookie` cho Refresh Token.
- **Backend ↔ PostgreSQL (Cloud):** JDBC qua SSL/TLS (`sslmode=require`).
- **Backend ↔ Redis (Cloud):** Redisson client qua TLS.

---

## III. CHIẾN LƯỢC XỬ LÝ ĐỒNG THỜI (CONCURRENCY ARCHITECTURE)

### Mô hình 2 tầng Lock (Defense in Depth):

```
Request đến
     │
     ▼
┌─────────────────────────────────────┐
│  TẦNG 1: Redis Distributed Lock     │  ← Chặn nhanh ở bộ nhớ đệm
│  Redisson MultiLock + Watchdog       │     (Microseconds latency)
│  Key: wallet:lock:{sortedWalletId}   │
│  tryLock(3s wait, -1 lease/watchdog) │
└──────────────┬──────────────────────┘
               │ Đã acquire lock thành công
               ▼
┌─────────────────────────────────────┐
│  TẦNG 2: PostgreSQL Row Lock         │  ← Phòng thủ chiều sâu tại DB
│  SELECT ... FOR UPDATE               │     (Milliseconds latency)
│  Lock theo thứ tự ID tăng dần        │
└──────────────┬──────────────────────┘
               │ Đã lock row thành công
               ▼
┌─────────────────────────────────────┐
│  THỰC THI NGHIỆP VỤ                  │
│  Check balance → Trừ/Cộng → Ghi log  │
│  COMMIT DB Transaction                │
└──────────────┬──────────────────────┘
               │ Transaction đã commit
               ▼
       Release Redis Lock
```

### Thứ tự thời gian bắt buộc:
1. **Acquire** Redis Lock
2. **Open** DB Transaction (`@Transactional` bắt đầu)
3. **Lock** DB Rows (SELECT FOR UPDATE)
4. **Execute** business logic
5. **Commit** DB Transaction (`@Transactional` kết thúc)
6. **Release** Redis Lock

> ⚠️ **CẢNH BÁO:** Nếu Release Redis Lock trước khi DB Commit, sẽ gây ra Race Condition dù đã dùng lock.

---

## IV. CHIẾN LƯỢC KẾT NỐI DATABASE CLOUD

### Lý do dùng Cloud DB thay vì Local:
1. Không cần cài đặt/quản lý PostgreSQL trên máy local.
2. Team nhiều người có thể truy cập cùng một instance để phát triển và test.
3. Connection String giống production — giảm rủi ro khác biệt môi trường.
4. Nền tảng cloud (Neon, Supabase) cung cấp Dashboard UI quản lý, backup, và monitoring miễn phí.

### Cấu hình kết nối từ Spring Boot:
```yaml
spring:
  datasource:
    url: ${DATABASE_URL}          # Lấy từ biến môi trường
    username: ${DATABASE_USERNAME}
    password: ${DATABASE_PASSWORD}
    hikari:
      maximum-pool-size: 10       # Cloud free tier thường giới hạn 10-20 connections
      minimum-idle: 2
      connection-timeout: 10000
      properties:
        sslmode: require          # Bắt buộc SSL khi kết nối cloud
```

### Lưu ý khi dùng Cloud DB:
- **Connection Pool phải nhỏ:** Free tier của Neon/Supabase thường giới hạn 10-25 connections đồng thời. Điều chỉnh `maximum-pool-size` cho phù hợp.
- **Độ trễ mạng:** Cloud DB sẽ có latency cao hơn local (20-100ms). Cần tối ưu số lượng query trong mỗi transaction.
- **SSL bắt buộc:** Mọi kết nối cloud DB đều yêu cầu `sslmode=require`.
