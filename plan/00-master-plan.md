# 00 - MASTER IMPLEMENTATION PLAN
## DỰ ÁN: MINI DIGITAL WALLET & QR PAYMENT HUB
> Hệ sinh thái ví điện tử mô phỏng — Bảo toàn giao dịch ACID, Chống Double-Spending & Thanh toán VietQR EMVCo

---

## I. TỔNG QUAN DỰ ÁN

### 1.1. Mục tiêu cốt lõi
Xây dựng hệ sinh thái thanh toán mô phỏng ví điện tử tiêu dùng, giải quyết các bài toán kỹ thuật cốt lõi:
- **Tính toàn vẹn giao dịch (ACID):** Mọi giao dịch tài chính phải Atomic — hoặc thành công toàn bộ hoặc rollback hoàn toàn.
- **Chống Double-Spending:** Không cho phép chi tiêu trùng lặp dù hàng trăm request đồng thời nhắm vào cùng một ví.
- **Thanh toán QR chuẩn quốc gia:** Sinh và giải mã mã VietQR theo đặc tả EMVCo Merchant-Presented Mode.

### 1.2. Tech Stack tổng thể

| Tầng | Công nghệ | Vai trò |
| :--- | :--- | :--- |
| **Backend API** | Java 21, Spring Boot 3.x, Spring Data JPA, Spring Security 6 | Xử lý nghiệp vụ, bảo mật, quản lý giao dịch |
| **Database** | PostgreSQL 16 (Cloud — Neon / Supabase) | Lưu trữ dữ liệu chính, ràng buộc ACID |
| **Cache & Lock** | Redis 7 (Cloud — Upstash / Redis Cloud) | Distributed Lock, Idempotency, Session Cache |
| **Migration** | Flyway | Quản lý phiên bản schema DB |
| **Web Admin** | Next.js 14+ (App Router), Vanilla CSS | Portal quản trị, giám sát, đối soát |
| **Mobile Client** | Java 17, Android SDK (API 26–35), MVVM | Ứng dụng ví điện tử cho người dùng cuối |
| **API Spec** | OpenAPI 3.0 (Swagger UI) | Tài liệu hóa & hợp đồng giao tiếp |
| **Testing** | JUnit 5, Mockito, Apache JMeter | Unit Test, Integration Test, Load Test |

---

## II. CẤU TRÚC THƯ MỤC PLAN (BẠN ĐANG Ở ĐÂY)

Mỗi file `.md` trong thư mục `plan/` là một **bản thiết kế chi tiết** cho một khía cạnh cụ thể. Bạn dùng từng file làm đầu vào để sinh (gen) code chi tiết.

```
plan/
│
├── 00-master-plan.md            ← [BẠN ĐANG ĐỌC FILE NÀY]
│                                   Tổng quan, mục lục, quy ước
│
├── 01-repo-strategy.md          ← Chiến lược Monorepo & Git Flow
│                                   (Cấu trúc Monorepo, branching Git Flow, Conventional Commits)
│
├── 02-architecture-plan.md      ← Kiến trúc hệ thống tổng thể
│                                   (Sơ đồ tầng, giao tiếp giữa các module)
│
├── 03-database-cloud.md         ← Thiết kế Database & Cloud Hosting
│                                   (Schema DDL, Flyway, Neon/Supabase setup)
│
├── 04-business-rules.md         ← Hệ thống ràng buộc quy tắc nghiệp vụ
│                                   (Quy tắc chung BR-GEN, quy tắc riêng BR-SPEC,
│                                    quy trình xây dựng rule mới)
│
├── 05-business-flows.md         ← Luồng nghiệp vụ End-to-End
│                                   (Sequence Diagram, State Machine, FLOW-xx)
│
├── 06-backend-plan.md           ← Kế hoạch Backend Spring Boot
│                                   (Package structure, service layers, config)
│
├── 07-frontend-plan.md          ← Kế hoạch Frontend Next.js Admin Portal
│                                   (Route structure, component tree, API integration)
│
├── 08-android-plan.md           ← Kế hoạch Mobile Android (Java)
│                                   (MVVM, CameraX, Biometric, Retrofit)
│
├── 09-api-contract.md           ← Hợp đồng API & Endpoint Catalog
│                                   (Request/Response specs, Error codes, Headers)
│
├── 10-security-plan.md          ← Kế hoạch Bảo mật toàn diện
│                                   (JWT, ECDSA, PIN protection, Keystore)
│
├── 11-testing-plan.md           ← Kế hoạch Kiểm thử & Đảm bảo chất lượng
│                                   (JMeter scenarios, Unit Test, Edge cases)
│
├── 12-deployment-plan.md        ← Quy trình Deploy & CI/CD Pipeline
│                                   (Build, Deploy BE/FE/Mobile, Release flow)
│
├── 13-session-lifecycle.md      ← Quản lý Phiên & Vòng đời Token
│                                   (Token lifecycle, Device binding, Biometric)
│
└── 14-roadmap.md                ← Lộ trình triển khai 6 giai đoạn
                                    (Phân chia tuần, tiêu chí nghiệm thu DoD)
```

---

## III. QUY ƯỚC CHUNG CHO TOÀN BỘ DỰ ÁN

### 3.1. Quy ước đặt tên (Naming Conventions)

| Ngữ cảnh | Quy ước | Ví dụ |
| :--- | :--- | :--- |
| Package Java (BE) | `com.walletapp.<module>` | `com.walletapp.wallet`, `com.walletapp.transaction` |
| Class Java | PascalCase | `WalletTransferService`, `IdempotencyFilter` |
| REST Endpoint | kebab-case, có prefix version | `/api/v1/wallets`, `/api/v1/transfers` |
| Database Table | snake_case, số nhiều | `users`, `wallets`, `ledger_entries` |
| Database Column | snake_case | `source_wallet_id`, `created_at` |
| Android Activity/Fragment | PascalCase + hậu tố | `QrScannerFragment`, `TransferActivity` |
| Next.js Route | kebab-case folder | `/dashboard`, `/users/[id]`, `/transactions` |
| Biến môi trường | UPPER_SNAKE_CASE | `DATABASE_URL`, `REDIS_URL`, `JWT_SECRET` |
| Git Branch | `type/ticket-description` | `feat/TX-01-p2p-transfer`, `fix/WAL-01-balance` |

### 3.2. Bảng mã chức năng (Feature Code Registry)

| Mã | Module | Tên Chức Năng |
| :--- | :--- | :--- |
| `AUTH-01` | Authentication | Đăng ký tài khoản & Tự động kích hoạt ví |
| `AUTH-02` | Authentication | Đăng nhập & Cấp phát JWT (Access + Refresh Token) |
| `AUTH-03` | Authentication | Đăng xuất & Thu hồi Token |
| `SEC-01` | Security | Đăng ký Public Key ECDSA từ Android Keystore |
| `SEC-02` | Security | Xác thực PIN & Sinh trắc học (BiometricPrompt) |
| `SEC-03` | Security | Chống Brute-force PIN (Redis Rate Limiter) |
| `WAL-01` | Wallet | Truy vấn số dư ví & Thông tin chủ ví |
| `WAL-02` | Wallet | Xem lịch sử giao dịch (phân trang, lọc) |
| `TX-01` | Transaction | Chuyển tiền P2P (Idempotency + MultiLock + Ledger kép) |
| `TX-02` | Transaction | Nạp tiền vào ví (Top-up giả lập) |
| `TX-03` | Transaction | Rút tiền về ngân hàng (Withdraw) |
| `QR-01` | QR Code | Sinh mã VietQR EMVCo (TLV + CRC16-CCITT) |
| `QR-02` | QR Code | Quét & Giải mã VietQR (CameraX + ML Kit) |
| `QR-03` | QR Code | Thanh toán qua mã QR đã quét |
| `ADM-01` | Admin | Dashboard tổng quan hệ thống |
| `ADM-02` | Admin | Quản lý tài khoản người dùng (tra cứu, khóa/mở ví) |
| `ADM-03` | Admin | Đối soát sổ cái kế toán kép |
| `ADM-04` | Admin | Cấu hình hạn mức & biểu phí giao dịch |

### 3.3. Ma trận Tác nhân (Actor Registry)

| Mã | Tên | Mô Tả | Nền tảng |
| :--- | :--- | :--- | :--- |
| `GUEST` | Khách vãng lai | Chưa đăng nhập, xem Landing/Login | Android, Web |
| `USER` | Chủ ví | Sở hữu ví, nạp/rút/chuyển/quét QR | Android |
| `ADMIN` | QTV nghiệp vụ | Kiểm tra, đối soát, xử lý khiếu nại | Web Admin |
| `SUPER_ADMIN` | QTV tối cao | Cấu hình hạn mức, biểu phí, phân quyền | Web Admin |
| `OWNER` | Chủ sở hữu | Xem báo cáo tài chính tổng quan, KPI | Web Admin |
| `SYSTEM` | Tiến trình ngầm | Watchdog lock, Worker QR hết hạn, Job đối soát | Backend |

### 3.4. Ma trận phân quyền RACI

| Mã Chức Năng | GUEST | USER | ADMIN | SUPER_ADMIN | OWNER | SYSTEM |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: |
| `AUTH-01` Đăng ký | R | I | - | - | - | A |
| `AUTH-02` Đăng nhập | R | A | - | - | - | I |
| `TX-01` Chuyển tiền P2P | - | R/A | I | - | I | R |
| `TX-02` Nạp tiền | - | R/A | I | - | - | R |
| `TX-03` Rút tiền | - | R/A | I | - | I | R |
| `QR-01` Sinh VietQR | - | R/A | - | - | - | R |
| `QR-02` Quét QR | - | R/A | - | - | - | R |
| `ADM-01` Dashboard | - | - | R | A | I | R |
| `ADM-02` Quản lý User | - | I | R | A | I | R |
| `ADM-03` Đối soát | - | - | R | A | I | R |
| `ADM-04` Cấu hình | - | - | C | R/A | I | R |

> **R** = Responsible (Thực thi) · **A** = Accountable (Phê duyệt) · **C** = Consulted (Tham vấn) · **I** = Informed (Nhận thông báo)

---

## IV. CÁCH SỬ DỤNG PLAN DIRECTORY

### Quy trình sử dụng:
1. Đọc file `00-master-plan.md` này trước để nắm tổng quan.
2. Chọn file plan cụ thể (ví dụ `06-backend-plan.md`).
3. Dùng nội dung file đó làm **prompt đầu vào** để gen code chi tiết.
4. Sau khi gen xong, kiểm tra đối chiếu với `04-business-rules.md` để đảm bảo tuân thủ ràng buộc.

### Tham chiếu nhanh giữa các file:

| Khi bạn cần... | Đọc file | Liên quan đến |
| :--- | :--- | :--- |
| Tạo repo Git, branching | `01-repo-strategy.md` | — |
| Hiểu kiến trúc tổng thể | `02-architecture-plan.md` | `06`, `07`, `08` |
| Tạo bảng DB, migration | `03-database-cloud.md` | `04`, `06` |
| Viết logic validate | `04-business-rules.md` | `05`, `06`, `09` |
| Hiểu luồng User → DB | `05-business-flows.md` | `04`, `09` |
| Code backend Spring Boot | `06-backend-plan.md` | `03`, `04`, `09` |
| Code frontend Next.js | `07-frontend-plan.md` | `09` |
| Code mobile Android | `08-android-plan.md` | `09`, `10` |
| Thiết kế API endpoints | `09-api-contract.md` | `04`, `05` |
| Thiết kế bảo mật | `10-security-plan.md` | `08`, `13` |
| Viết test & load test | `11-testing-plan.md` | `04`, `05` |
| Deploy lên cloud | `12-deployment-plan.md` | `01`, `06`, `07` |
| Quản lý token & session | `13-session-lifecycle.md` | `10` |
| Xem lộ trình tổng | `14-roadmap.md` | Tất cả |
