# 💳 Mini Digital Wallet & QR Payment Hub

> **Hệ sinh thái thanh toán ví điện tử tiêu dùng mô phỏng**  
> Giải quyết bài toán cốt lõi về **tính toàn vẹn giao dịch (ACID)**, **chống Double-Spending (chi tiêu trùng lặp)** bằng Distributed Lock (Redis Redisson) + Pessimistic Write Lock, và **thanh toán VietQR theo đặc tả EMVCo**.

---

## 🏗️ Kiến trúc Hệ sinh thái (Monorepo)

Dự án được tổ chức theo cấu trúc **Monorepo** tích hợp toàn diện từ tài liệu thiết kế, backend, frontend đến ứng dụng di động:

```text
Mini Digital Wallet & QR Payment Hub/
├── plan/                    # 15 tài liệu kế hoạch chi tiết, quy tắc nghiệp vụ, API contract & security
├── digital-wallet-api/      # Backend RESTful API (Java 22, Spring Boot 3.x, Flyway, PostgreSQL, Redis)
├── digital-wallet-admin/    # Portal quản trị hệ thống (Next.js 16, TypeScript, Vanilla CSS Modules)
└── digital-wallet-android/  # Ứng dụng di động ví điện tử (Java 17, Android SDK API 26-35, MVVM)
```

---

## 🛠️ Công nghệ cốt lõi (Tech Stack)

| Tầng | Công nghệ chính | Vai trò kỹ thuật |
| :--- | :--- | :--- |
| **Backend API** | Java 22, Spring Boot 3.3.x, Spring Data JPA, Spring Security 6 | Xử lý logic tài chính, chống tấn công Race Condition, cấp phát JWT |
| **Distributed Lock & Cache** | Redis 7 (Upstash Serverless Cloud với TLS) | Distributed Lock (Redisson MultiLock), Idempotency Key, Session Store |
| **Database** | PostgreSQL 16 (Neon Serverless Cloud) | Lưu trữ sổ cái (Ledger), ràng buộc ACID, kiểm soát số dư không âm |
| **Database Migration** | Flyway | Quản lý phiên bản schema tự động (`V1`, `V2`...) |
| **Frontend Web Admin** | Next.js 16 (App Router), TypeScript, Vanilla CSS | Dashboard giám sát dòng tiền, tra cứu lịch sử, cấu hình hạn mức & biểu phí |
| **Mobile Client** | Java 17, Android SDK (minSdk 26, targetSdk 35), MVVM | Giao diện ví người dùng, CameraX quét VietQR, Biometric & Android Keystore |

---

## 🚀 Hướng dẫn khởi chạy nhanh (Quick Start)

### 1. Backend (Spring Boot API)
```powershell
cd digital-wallet-api
mvn clean compile
mvn spring-boot:run
# API sẵn sàng tại: http://localhost:8080
```

### 2. Frontend (Next.js Web Admin)
```powershell
cd digital-wallet-admin
npm run dev
# Dashboard mở tại: http://localhost:3000
```

### 3. Mobile (Android App)
* Mở thư mục `digital-wallet-android` trong **Android Studio**.
* Chọn máy ảo Emulator (API 26+) và bấm nút **Run ▶** (Shift + F10).

---

## 📚 Tài liệu chi tiết
Toàn bộ quy chuẩn kiến trúc và luồng nghiệp vụ nằm trong thư mục [`plan/`](./plan):
* [`00-master-plan.md`](./plan/00-master-plan.md) — Tổng quan kiến trúc & ma trận RACI
* [`02-architecture-plan.md`](./plan/02-architecture-plan.md) — Kiến trúc 2 tầng khóa (Redis + Pessimistic Lock)
* [`03-database-cloud.md`](./plan/03-database-cloud.md) — Thiết kế cơ sở dữ liệu & sơ đồ ERD
* [`05-business-flows.md`](./plan/05-business-flows.md) — 9 luồng nghiệp vụ End-to-End & State Machine giao dịch
* [`09-api-contract.md`](./plan/09-api-contract.md) — Danh mục API endpoints & Error codes
* [`10-security-plan.md`](./plan/10-security-plan.md) — Mô hình bảo mật đa tầng (Defense in Depth)
* [`14-roadmap.md`](./plan/14-roadmap.md) — Lộ trình triển khai chi tiết 6 giai đoạn
