# 01 - QUY TRÌNH TẠO REPOSITORY & QUẢN LÝ GIT

---

## I. CHIẾN LƯỢC REPOSITORY (REPO STRATEGY)

### 1.1. Mô hình Multi-Repo (Tách biệt 3 repo)
Dự án sử dụng mô hình **Multi-Repository** — mỗi tầng ứng dụng là một repo độc lập, phù hợp khi team nhỏ và mỗi thành phần có lifecycle deploy riêng.

| Repo | Tên Gợi Ý | Ngôn Ngữ | Mô Tả |
| :--- | :--- | :--- | :--- |
| **Backend** | `digital-wallet-api` | Java 21 / Spring Boot 3 | REST API, nghiệp vụ ví, Distributed Lock |
| **Web Admin** | `digital-wallet-admin` | TypeScript / Next.js 14 | Portal quản trị cho Admin/SuperAdmin/Owner |
| **Mobile** | `digital-wallet-android` | Java 17 / Android | Ứng dụng ví cho người dùng cuối (USER) |

### 1.2. Cấu trúc thư mục gợi ý từng Repo

#### A. `digital-wallet-api` (Backend)
```
digital-wallet-api/
├── src/
│   ├── main/
│   │   ├── java/com/walletapp/
│   │   │   ├── config/          # Cấu hình Spring, Redis, Security
│   │   │   ├── auth/            # Module AUTH-01, AUTH-02, AUTH-03
│   │   │   ├── wallet/          # Module WAL-01, WAL-02
│   │   │   ├── transaction/     # Module TX-01, TX-02, TX-03
│   │   │   ├── qrcode/          # Module QR-01
│   │   │   ├── admin/           # Module ADM-01 đến ADM-04
│   │   │   ├── security/        # Module SEC-01, SEC-02, SEC-03
│   │   │   └── common/          # Exception handler, DTO base, Utils
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       └── db/migration/    # Flyway SQL files
│   └── test/
│       └── java/com/walletapp/
│           ├── unit/            # Unit Tests
│           └── integration/     # Integration Tests
├── jmeter/
│   └── load-test-plan.jmx      # Kịch bản JMeter 500 threads
├── .env.example
├── .gitignore
├── pom.xml
└── README.md
```

#### B. `digital-wallet-admin` (Frontend Next.js)
```
digital-wallet-admin/
├── src/
│   ├── app/
│   │   ├── layout.tsx
│   │   ├── page.tsx             # Landing / Login
│   │   ├── dashboard/           # ADM-01: Tổng quan
│   │   ├── users/               # ADM-02: Quản lý người dùng
│   │   │   └── [id]/
│   │   ├── transactions/        # ADM-03: Đối soát giao dịch
│   │   └── settings/            # ADM-04: Cấu hình hạn mức
│   ├── components/
│   ├── lib/                     # API client, auth helpers
│   └── styles/                  # Vanilla CSS modules
├── public/
├── .env.local.example
├── .gitignore
├── package.json
└── README.md
```

#### C. `digital-wallet-android` (Mobile)
```
digital-wallet-android/
├── app/
│   ├── src/main/
│   │   ├── java/com/walletapp/android/
│   │   │   ├── data/            # Repository, Retrofit API, Local DB
│   │   │   ├── domain/          # Use Cases, Models
│   │   │   ├── ui/
│   │   │   │   ├── auth/        # Login, Register screens
│   │   │   │   ├── home/        # Dashboard, Balance
│   │   │   │   ├── transfer/    # P2P Transfer
│   │   │   │   ├── qrscanner/   # CameraX + ML Kit
│   │   │   │   ├── qrgenerate/ # Tạo mã QR
│   │   │   │   └── history/     # Lịch sử giao dịch
│   │   │   ├── security/        # Keystore, Biometric helpers
│   │   │   └── util/            # VietQR TLV parser, Network utils
│   │   ├── res/
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── .gitignore
└── README.md
```

---

## II. QUY TRÌNH TẠO REPOSITORY

### Bước 1: Tạo Repo trên GitHub/GitLab
```bash
# 1. Tạo 3 repo trên GitHub (Public hoặc Private)
#    - digital-wallet-api
#    - digital-wallet-admin
#    - digital-wallet-android

# 2. Clone về máy local
git clone https://github.com/<username>/digital-wallet-api.git
git clone https://github.com/<username>/digital-wallet-admin.git
git clone https://github.com/<username>/digital-wallet-android.git
```

### Bước 2: Thiết lập `.gitignore` chuẩn cho từng repo

#### Backend `.gitignore`:
```gitignore
# Build
target/
*.class
*.jar

# IDE
.idea/
*.iml
.vscode/

# Environment
.env
application-local.yml

# OS
.DS_Store
Thumbs.db

# Logs
*.log
logs/
```

#### Frontend `.gitignore`:
```gitignore
node_modules/
.next/
out/
.env.local
.env*.local
*.tsbuildinfo
```

#### Android `.gitignore`:
```gitignore
*.iml
.gradle/
/local.properties
/.idea/
/build/
/app/build/
/captures/
.externalNativeBuild/
.cxx/
*.apk
*.aab
```

### Bước 3: Tạo nhánh `develop` & Thiết lập Branch Protection
```bash
# Trong mỗi repo
git checkout -b develop
git push -u origin develop

# Trên GitHub → Settings → Branches → Add rule:
#   Branch name pattern: main
#   ✅ Require pull request reviews before merging (1 reviewer)
#   ✅ Require status checks to pass before merging
#   ✅ Do not allow force pushes
```

---

## III. CHIẾN LƯỢC PHÂN NHÁNH (BRANCHING STRATEGY)

### Mô hình Git Flow đơn giản hóa:

```
main ──────────────────────────────────────── (Production-ready)
  │
  └── develop ─────────────────────────────── (Tích hợp liên tục)
        │
        ├── feat/AUTH-01-register ──────────── (Chức năng mới)
        ├── feat/TX-01-p2p-transfer
        ├── feat/QR-01-vietqr-generator
        │
        ├── fix/WAL-01-balance-race ────────── (Sửa lỗi)
        │
        ├── hotfix/SEC-03-pin-bypass ───────── (Vá lỗi khẩn cấp)
        │
        └── release/v1.0.0 ────────────────── (Chuẩn bị phát hành)
```

### Quy ước đặt tên nhánh:

| Loại | Format | Ví dụ |
| :--- | :--- | :--- |
| Chức năng mới | `feat/<MÃ>-<mô-tả>` | `feat/TX-01-p2p-transfer` |
| Sửa lỗi | `fix/<MÃ>-<mô-tả>` | `fix/WAL-01-balance-negative` |
| Vá khẩn cấp | `hotfix/<MÃ>-<mô-tả>` | `hotfix/SEC-03-pin-bruteforce` |
| Phát hành | `release/v<major>.<minor>.<patch>` | `release/v1.0.0` |
| Cải thiện code | `refactor/<mô-tả>` | `refactor/extract-lock-service` |

---

## IV. QUY TẮC COMMIT MESSAGE

### Format chuẩn (Conventional Commits):
```
<type>(<scope>): <mô tả ngắn gọn>

[Nội dung bổ sung nếu cần]

[Footer: issue reference]
```

### Bảng `type` cho phép:

| Type | Mô Tả | Ví dụ |
| :--- | :--- | :--- |
| `feat` | Chức năng mới | `feat(TX-01): implement P2P transfer with MultiLock` |
| `fix` | Sửa lỗi | `fix(WAL-01): prevent negative balance on concurrent withdraw` |
| `refactor` | Tái cấu trúc (không thêm/sửa logic) | `refactor(security): extract ECDSA signer to util class` |
| `test` | Thêm/sửa test | `test(TX-01): add 500-thread JMeter load test` |
| `docs` | Tài liệu | `docs(api): update Swagger spec for /transfers` |
| `chore` | Config, CI/CD, tool | `chore(ci): add GitHub Actions build workflow` |
| `perf` | Tối ưu hiệu năng | `perf(db): add composite index on transactions table` |

---

## V. QUY TRÌNH PULL REQUEST (PR)

### Template PR:
```markdown
## Mô tả
<!-- Mô tả ngắn gọn thay đổi -->

## Mã chức năng liên quan
<!-- VD: TX-01, QR-02 -->

## Loại thay đổi
- [ ] Chức năng mới (feat)
- [ ] Sửa lỗi (fix)
- [ ] Refactor
- [ ] Cấu hình / CI/CD

## Checklist
- [ ] Code biên dịch thành công (không có error/warning)
- [ ] Đã viết Unit Test cho logic mới
- [ ] Đã kiểm tra tuân thủ Business Rules (BR-GEN / BR-SPEC)
- [ ] Đã cập nhật API contract (nếu thay đổi endpoint)
- [ ] Đã test trên Emulator / Trình duyệt (nếu là FE/Mobile)

## Screenshots (nếu có giao diện)
```

### Quy trình Review:
1. Developer tạo PR từ nhánh `feat/*` hoặc `fix/*` vào `develop`.
2. Tự kiểm tra Checklist trước khi gán Reviewer.
3. Reviewer kiểm tra code, chạy thử (nếu cần), và Approve hoặc Request Changes.
4. Merge vào `develop` bằng **Squash and Merge** (gộp commit cho gọn lịch sử).
5. Khi `develop` ổn định, tạo nhánh `release/vX.Y.Z`, test cuối cùng, rồi merge vào `main`.

---

## VI. QUẢN LÝ BIẾN MÔI TRƯỜNG

### Nguyên tắc:
- **KHÔNG BAO GIỜ** commit file `.env`, `application-local.yml` hoặc bất kỳ file nào chứa credential thật vào Git.
- Luôn có file `.env.example` hoặc `application.yml.example` chứa key mẫu (không có giá trị thật) để hướng dẫn developer mới.

### File `.env.example` mẫu cho Backend:
```env
# Database (Cloud PostgreSQL)
DATABASE_URL=jdbc:postgresql://<host>:<port>/<dbname>?sslmode=require
DATABASE_USERNAME=<username>
DATABASE_PASSWORD=<password>

# Redis (Cloud)
REDIS_URL=redis://default:<password>@<host>:<port>

# JWT
JWT_SECRET=<your-256-bit-secret>
JWT_ACCESS_EXPIRY=900000
JWT_REFRESH_EXPIRY=604800000

# App
APP_PORT=8080
APP_ENV=development
```
