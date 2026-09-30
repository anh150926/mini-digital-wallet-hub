# 01 - QUY TRÌNH QUẢN LÝ REPOSITORY & GIT FLOW (MONOREPO)

---

## I. CHIẾN LƯỢC MONOREPO (UNIFIED REPOSITORY)

### 1.1. Lựa chọn mô hình Monorepo cho Dự án Cá nhân
Dự án sử dụng mô hình **Single Monorepo** — gom toàn bộ hệ sinh thái ví điện tử (Tài liệu kiến trúc, Backend Spring Boot, Web Admin Next.js, và Mobile App Android) vào **1 Repository duy nhất** mang tên `mini-digital-wallet-hub`.

| Lợi ích kỹ thuật | Chi tiết |
| :--- | :--- |
| **Portfolio CV hoàn hảo** | Nhà tuyển dụng chỉ cần 1 link GitHub là thấy toàn bộ kiến trúc Full-stack từ A-Z. |
| **Tiện lợi khi code 1 mình** | Cùng một tính năng (vd: Chuyển tiền P2P), có thể commit đồng bộ cả API backend và UI mobile trong cùng 1 lần git commit. |
| **Độc lập triển khai (Deploy)** | Vercel và Render/Railway đều hỗ trợ thiết lập `Root Directory` độc lập từ 1 repo chung. |
| **Quản lý tài liệu tập trung** | Thư mục `plan/` nằm chung với mã nguồn, đảm bảo tài liệu luôn đồng hành cùng code. |

---

### 1.2. Cấu trúc thư mục Monorepo toàn diện

```text
mini-digital-wallet-hub/                         ← REPO GỐC (GitHub: anh150926/mini-digital-wallet-hub)
│
├── .gitignore                                   ← Bộ lọc rác tổng (Java, Node, Android, bảo vệ .env)
├── README.md                                    ← Bìa giới thiệu hệ sinh thái chuẩn Portfolio quốc tế
│
├── plan/                                        ← 15 TÀI LIỆU KẾ HOẠCH & QUY TẮC NGHIỆP VỤ
│   ├── 00-master-plan.md                        # Tổng quan, mục lục, bảng mã, RACI
│   ├── 01-repo-strategy.md                      # Chiến lược Monorepo & Git Flow
│   ├── 02-architecture-plan.md                  # Sơ đồ kiến trúc 2 tầng khóa (Redis + Pessimistic Lock)
│   ├── 03-database-cloud.md                     # Thiết kế PostgreSQL Cloud (Neon) & Redis (Upstash)
│   ├── 04-business-rules.md                     # Quy tắc nghiệp vụ chung & riêng từng module
│   ├── 05-business-flows.md                     # 9 luồng nghiệp vụ End-to-End & State Machine
│   ├── 06-backend-plan.md                       # Bản thiết kế chi tiết Spring Boot API
│   ├── 07-frontend-plan.md                      # Bản thiết kế chi tiết Next.js Admin Portal
│   ├── 08-android-plan.md                       # Bản thiết kế chi tiết Mobile Android Java
│   ├── 09-api-contract.md                       # Hợp đồng API OpenAPI 3.0 & mã lỗi Error Codes
│   ├── 10-security-plan.md                      # Mô hình bảo mật 6 lớp Defense in Depth
│   ├── 11-testing-plan.md                       # Kế hoạch kiểm thử & Kịch bản JMeter 500 threads
│   ├── 12-deployment-plan.md                    # Quy trình deploy Cloud & CI/CD Pipeline
│   ├── 13-session-lifecycle.md                  # Quản lý phiên, Refresh Token & Device Binding
│   └── 14-roadmap.md                            # Lộ trình triển khai 6 giai đoạn
│
├── digital-wallet-api/                          ← BACKEND (Java 22, Spring Boot 3.3.x)
│   ├── src/main/java/com/walletapp/
│   │   ├── config/                              # SecurityConfig, RedisConfig, OpenApiConfig
│   │   ├── common/                              # DTO wrapper, GlobalExceptionHandler, CryptoUtils
│   │   ├── auth/                                # Module AUTH (Register, Login, Token)
│   │   ├── wallet/                              # Module WAL (Balance, History, Freeze)
│   │   ├── transaction/                         # Module TX (TransferOrchestrator, TransferExecutor)
│   │   ├── qrcode/                              # Module QR (VietQR EMVCo Generator, QR Payment)
│   │   ├── security/                            # Module SEC (DeviceKey, PinService, ECDSA Verify)
│   │   └── admin/                               # Module ADM (Dashboard, Users, Reconcile, Configs)
│   ├── src/main/resources/
│   │   ├── application.yml                      # Cấu hình gốc
│   │   ├── application-dev.yml                  # Kết nối Neon DB & Upstash Redis Cloud
│   │   └── db/migration/                        # Flyway SQL (V1__create_core_tables, V2__seed_configs)
│   ├── .env                                     # Thông tin mật khẩu DB/Redis (được .gitignore bảo vệ)
│   └── pom.xml                                  # Quản lý thư viện Maven
│
├── digital-wallet-admin/                        ← FRONTEND WEB (Next.js 16, TypeScript, Vanilla CSS)
│   ├── src/app/
│   │   ├── dashboard/                           # ADM-01: Bảng điều khiển giám sát dòng tiền
│   │   ├── users/                               # ADM-02: Quản lý ví & khóa tài khoản
│   │   ├── transactions/                        # ADM-03: Đối soát bút toán Debit vs Credit
│   │   ├── settings/                            # ADM-04: Cấu hình hạn mức & biểu phí
│   │   └── layout.tsx, page.tsx
│   ├── src/components/                          # Reusable UI components
│   ├── src/lib/                                 # API client layer, Auth helpers
│   ├── src/styles/                              # CSS Modules
│   └── package.json
│
└── digital-wallet-android/                      ← MOBILE CLIENT (Java 17, Android SDK API 26-35)
    ├── app/src/main/java/com/walletapp/android/
    │   ├── data/                                # Retrofit API, Local EncryptedStorage
    │   ├── ui/                                  # Auth, Home, Transfer, QrScanner, History
    │   ├── security/                            # Android Keystore, BiometricPrompt, ECDSA Key
    │   └── util/                                # VietQR TLV Parser, NetworkUtils
    ├── app/src/main/res/                        # Layout XML, Drawables, Values
    ├── build.gradle.kts                         # Cấu hình Gradle
    └── AndroidManifest.xml                      # Quyền Camera, Biometric, Internet
```

---

## II. CHIẾN LƯỢC PHÂN NHÁNH GIT FLOW (BRANCHING STRATEGY)

Dự án áp dụng mô hình **Git Flow chuẩn hóa** trên toàn bộ Monorepo:

```text
main ─────────────────────────────────────────────────────────── (Production-ready)
  │
  └── develop ────────────────────────────────────────────────── (Tích hợp liên tục hệ sinh thái)
        │
        ├── feat/core-01-jpa-entities ────────── (Làm JPA Entities cho 7 bảng)
        ├── feat/api-auth-register ───────────── (Làm API Đăng ký)
        ├── feat/api-tx-transfer ─────────────── (Làm Engine Chuyển tiền + MultiLock)
        ├── feat/admin-dashboard ─────────────── (Làm Dashboard Web Next.js)
        ├── feat/android-qr-scanner ──────────── (Làm CameraX quét VietQR)
        │
        ├── fix/api-wallet-balance-race ──────── (Sửa lỗi concurrency)
        │
        └── release/v1.0.0 ───────────────────── (Chuẩn bị phát hành phiên bản)
```

### 2.1. Quy ước đặt tên nhánh trong Monorepo:

| Loại nhánh | Format chuẩn | Ví dụ thực tế |
| :--- | :--- | :--- |
| **Tính năng Backend** | `feat/api-<mã-chức-năng>` | `feat/api-auth-register`, `feat/api-tx-p2p` |
| **Tính năng Frontend** | `feat/admin-<tên-trang>` | `feat/admin-dashboard`, `feat/admin-users` |
| **Tính năng Mobile** | `feat/android-<tên-chức-năng>` | `feat/android-qr-scan`, `feat/android-biometric` |
| **Tính năng chung** | `feat/core-<tên-thành-phần>` | `feat/core-jpa-entities`, `feat/core-db-schema` |
| **Sửa lỗi** | `fix/<phạm-vi>-<mô-tả>` | `fix/api-balance-race`, `fix/android-camera-crash` |
| **Vá lỗi khẩn cấp** | `hotfix/<mô-tả>` | `hotfix/pin-bruteforce-bypass` |
| **Phát hành** | `release/v<major>.<minor>.<patch>` | `release/v1.0.0` |

---

## III. QUY TẮC COMMIT MESSAGE (CONVENTIONAL COMMITS)

Mỗi commit phải chỉ rõ **Scope (phạm vi ảnh hưởng)** trong Monorepo:

### Cú pháp:
```text
<type>(<scope>): <mô tả ngắn gọn bằng tiếng Anh hoặc tiếng Việt>
```

### Bảng Scope quy định trong Monorepo:
* **`api`**: Thay đổi trong thư mục `digital-wallet-api/`
* **`admin`**: Thay đổi trong thư mục `digital-wallet-admin/`
* **`android`**: Thay đổi trong thư mục `digital-wallet-android/`
* **`docs`**: Thay đổi trong thư mục `plan/` hoặc file `README.md`
* **`root`**: Thay đổi ở root (`.gitignore`, `.github/`, config chung)

### Ví dụ Commit chuẩn:
* `feat(api): implement TransferOrchestrator with Redisson MultiLock`
* `feat(android): add CameraX QR scanner with ML Kit parser`
* `feat(admin): build reconciliation report table with Debit/Credit balance`
* `fix(api): prevent deadlock by sorting wallet IDs ascending before lock`
* `docs(plan): update roadmap DoD for phase 1 completion`
* `chore(root): configure unified monorepo gitignore`

---

## IV. QUẢN LÝ BẢO MẬT BIẾN MÔI TRƯỜNG TRONG MONOREPO

1. **File `.gitignore` gốc**:
   * Chặn tuyệt đối mọi file `.env`, `digital-wallet-api/.env`, `digital-wallet-android/local.properties`.
   * Chặn toàn bộ rác build (`target/`, `node_modules/`, `.next/`, `build/`, `.gradle/`).
2. **File `.env.example`**:
   * Luôn duy trì file mẫu không chứa mật khẩu thật để lưu cấu trúc các biến cần thiết:
     * `DATABASE_URL=jdbc:postgresql://<host>:<port>/<dbname>?sslmode=require`
     * `REDIS_URL=rediss://default:<password>@<host>:<port>`
     * `JWT_SECRET=<your-256-bit-secret>`
3. **Môi trường Cloud (Production)**:
   * Mật khẩu thật được nhập trực tiếp vào mục **Environment Variables** trên Dashboard của Render/Vercel, hoàn toàn không đẩy lên Git.
