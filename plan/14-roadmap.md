# 14 - LỘ TRÌNH TRIỂN KHAI CHI TIẾT (ROADMAP)

---

## TỔNG QUAN 6 GIAI ĐOẠN

```
Tuần 1          Tuần 2          Tuần 3          Tuần 4          Tuần 5          Tuần 6
┌───────┐      ┌───────┐      ┌───────┐      ┌───────┐      ┌───────┐      ┌───────┐
│  GĐ1  │ ──> │  GĐ2  │ ──> │  GĐ3  │ ──> │  GĐ4  │ ──> │  GĐ5  │ ──> │  GĐ6  │
│       │      │       │      │       │      │       │      │       │      │       │
│ Nền   │      │ Core  │      │Mobile │      │ Web   │      │ Test  │      │Deploy │
│ Tảng  │      │Engine │      │Android│      │Admin  │      │ Load  │      │& Báo  │
│       │      │       │      │       │      │       │      │       │      │ Cáo   │
└───────┘      └───────┘      └───────┘      └───────┘      └───────┘      └───────┘
```

---

## GIAI ĐOẠN 1: NỀN TẢNG & HẠ TẦNG (Tuần 1)

### Mục tiêu:
Thiết lập toàn bộ hạ tầng cơ sở: Repository, Cloud DB, Cloud Redis, và khung dự án.

### Nhiệm vụ chi tiết:

| # | Nhiệm vụ | File Plan tham chiếu | Thời gian |
| :--- | :--- | :--- | :--- |
| 1.1 | Tạo Monorepo GitHub (`mini-digital-wallet-hub`) | `01-repo-strategy.md` | 1h |
| 1.2 | Cấu hình Monorepo `.gitignore`, README, Git Flow | `01-repo-strategy.md` | 1h |
| 1.3 | Đăng ký Neon.tech, tạo PostgreSQL database | `03-database-cloud.md` | 30 min |
| 1.4 | Đăng ký Upstash, tạo Redis instance | `03-database-cloud.md` | 30 min |
| 1.5 | Khởi tạo project Spring Boot (Java 22, Maven) | `06-backend-plan.md` | 1h |
| 1.6 | Cấu hình `application.yml` kết nối Cloud DB + Redis | `06-backend-plan.md` | 1h |
| 1.7 | Viết Flyway migration `V1__create_core_tables.sql` | `03-database-cloud.md` | 2h |
| 1.8 | Viết Flyway migration `V2__seed_system_configs.sql` | `03-database-cloud.md` | 30 min |
| 1.9 | Chạy Flyway migrate → Đã tạo 7 bảng trên Neon | `03-database-cloud.md` | 30 min |
| 1.10 | Viết JPA Entities cho tất cả bảng (User, Wallet, Transaction, LedgerEntry, QrCode, DeviceKey, SystemConfig) | `06-backend-plan.md` | 3h |

### Tiêu chí nghiệm thu (Definition of Done):
- [x] Monorepo GitHub đã tạo, có `.gitignore` và nhánh `develop`.
- [x] Cloud PostgreSQL (Neon) chạy, có 7 bảng với đầy đủ constraints.
- [x] Cloud Redis (Upstash) chạy, kết nối TLS thành công.
- [x] Spring Boot khởi động thành công, kết nối được cả DB và Redis.
- [x] `system_configs` có 10 record cấu hình mặc định.
- [ ] JPA Entities được ánh xạ đầy đủ, validate schema thành công.

---

## GIAI ĐOẠN 2: CORE BACKEND ENGINE (Tuần 2)

### Mục tiêu:
Xây dựng toàn bộ API cốt lõi: Auth, Transfer (với MultiLock + Idempotency), VietQR Generator.

### Nhiệm vụ chi tiết:

| # | Nhiệm vụ | File Plan tham chiếu | Thời gian |
| :--- | :--- | :--- | :--- |
| 2.1 | Module AUTH: Register + Login + JWT Service | `06-backend-plan.md`, `09-api-contract.md` | 4h |
| 2.2 | Security: JWT Filter + Spring Security Config | `10-security-plan.md` | 3h |
| 2.3 | Module WALLET: Get Balance + Transaction History | `06-backend-plan.md`, `09-api-contract.md` | 2h |
| 2.4 | Idempotency Filter (Redis) | `04-business-rules.md` (BR-GEN-02) | 3h |
| 2.5 | TransferOrchestrator: MultiLock + Lock Ordering | `06-backend-plan.md`, `05-business-flows.md` (FLOW-03) | 4h |
| 2.6 | TransferExecutor: @Transactional, Debit/Credit, Ledger | `04-business-rules.md` (BR-GEN-01, BR-GEN-05) | 3h |
| 2.7 | LimitValidationService: Kiểm tra hạn mức | `04-business-rules.md` (BR-SPEC-TX01) | 2h |
| 2.8 | FeeCalculationService | `04-business-rules.md` (BR-SPEC-TX03) | 1h |
| 2.9 | PinService: Verify PIN + Redis Fail Counter | `04-business-rules.md` (BR-SPEC-SEC01) | 2h |
| 2.10 | VietQR Generator (TLV + Nested Tag 38 + CRC16) | `09-api-contract.md` (QR-01) | 3h |
| 2.11 | QR Payment Service | `05-business-flows.md` (FLOW-07) | 2h |
| 2.12 | GlobalExceptionHandler + Error Codes | `04-business-rules.md` (Bảng Error Codes) | 2h |
| 2.13 | Swagger/OpenAPI Config | `09-api-contract.md` | 1h |
| 2.14 | Unit Tests cho tất cả rules (BR-GEN + BR-SPEC) | `11-testing-plan.md` | 4h |

### Tiêu chí nghiệm thu:
- [ ] Đăng ký → Đăng nhập → Lấy số dư: Hoạt động end-to-end qua Postman.
- [ ] Chuyển tiền P2P: Idempotency + MultiLock hoạt động đúng.
- [ ] VietQR sinh ra chuỗi TLV đúng chuẩn EMVCo (kiểm tra CRC16).
- [ ] Nhập sai PIN 5 lần → Tài khoản bị khóa trong Redis.
- [ ] Tất cả Unit Tests pass.

---

## GIAI ĐOẠN 3: MOBILE ANDROID (Tuần 3)

### Mục tiêu:
Xây dựng ứng dụng Android đầy đủ: Đăng nhập, Dashboard, Chuyển tiền, Quét/Tạo QR.

### Nhiệm vụ chi tiết:

| # | Nhiệm vụ | File Plan tham chiếu | Thời gian |
| :--- | :--- | :--- | :--- |
| 3.1 | Project Setup: Dependencies, ViewBinding, Navigation | `08-android-plan.md` | 2h |
| 3.2 | SecureStorage (EncryptedSharedPreferences) | `08-android-plan.md` | 1h |
| 3.3 | Retrofit ApiService + AuthInterceptor + TokenRefresh | `08-android-plan.md` | 3h |
| 3.4 | LoginActivity + RegisterActivity (UI + ViewModel) | `08-android-plan.md` | 3h |
| 3.5 | HomeFragment: Hiển thị số dư, Quick Actions | `08-android-plan.md` | 2h |
| 3.6 | TransferFragment → ConfirmFragment → ResultFragment | `08-android-plan.md` | 4h |
| 3.7 | BiometricHelper + KeystoreManager (ECDSA) | `08-android-plan.md`, `10-security-plan.md` | 3h |
| 3.8 | QrGenerateFragment: Gọi API tạo QR + Hiển thị bitmap | `08-android-plan.md` | 2h |
| 3.9 | QrScannerFragment: CameraX + ML Kit + TLV Parser | `08-android-plan.md` | 4h |
| 3.10 | HistoryFragment: RecyclerView + Pagination | `08-android-plan.md` | 2h |
| 3.11 | VietQrParser (Parse TLV + Verify CRC16 trên client) | `04-business-rules.md` (BR-SPEC-QR03) | 2h |

### Tiêu chí nghiệm thu:
- [ ] Đăng nhập thành công trên Emulator, token lưu vào EncryptedSharedPrefs.
- [ ] Dashboard hiển thị đúng số dư, 4 nút Quick Action hoạt động.
- [ ] Chuyển tiền: Quét vân tay → Ký ECDSA → API trả 200 OK.
- [ ] Camera quét được QR VietQR trong < 0.5 giây.
- [ ] App hoạt động trên thiết bị thật (USB debug).

---

## GIAI ĐOẠN 4: WEB ADMIN PORTAL (Tuần 4)

### Mục tiêu:
Xây dựng giao diện quản trị cho Admin/SuperAdmin/Owner.

### Nhiệm vụ chi tiết:

| # | Nhiệm vụ | File Plan tham chiếu | Thời gian |
| :--- | :--- | :--- | :--- |
| 4.1 | Khởi tạo Next.js project, CSS Design System | `07-frontend-plan.md` | 2h |
| 4.2 | Login page + Auth Guard + Token management | `07-frontend-plan.md`, `13-session-lifecycle.md` | 3h |
| 4.3 | Admin Layout: Sidebar + Topbar + Breadcrumb | `07-frontend-plan.md` | 3h |
| 4.4 | Dashboard page (ADM-01): Stats cards + Line chart | `07-frontend-plan.md` | 4h |
| 4.5 | Users page (ADM-02): Table + Search + Detail | `07-frontend-plan.md` | 4h |
| 4.6 | Freeze/Unfreeze Wallet modal | `05-business-flows.md` (FLOW-08) | 2h |
| 4.7 | Transactions page (ADM-03): Filter + Table | `07-frontend-plan.md` | 3h |
| 4.8 | Reconciliation page (ADM-03): Debit vs Credit report | `07-frontend-plan.md` | 3h |
| 4.9 | Settings page (ADM-04): System configs form | `07-frontend-plan.md` | 2h |
| 4.10 | Backend: Admin API endpoints (ADM-01~04) | `09-api-contract.md` | 4h |

### Tiêu chí nghiệm thu:
- [ ] Admin đăng nhập → Thấy Dashboard với dữ liệu thật từ API.
- [ ] Admin bấm khóa ví → Ví bị FROZEN → Mobile gọi API bị từ chối tức thì.
- [ ] Trang Reconciliation hiển thị Σ DEBIT = Σ CREDIT (net = 0) cho giao dịch P2P.
- [ ] USER role truy cập `/admin` → Bị redirect về Login.

---

## GIAI ĐOẠN 5: KIỂM THỬ TẢI & TÍCH HỢP (Tuần 5)

### Mục tiêu:
Kiểm thử tải trọng, edge cases, và tích hợp toàn hệ thống.

### Nhiệm vụ chi tiết:

| # | Nhiệm vụ | File Plan tham chiếu | Thời gian |
| :--- | :--- | :--- | :--- |
| 5.1 | Viết kịch bản JMeter: 500 threads rút tiền | `11-testing-plan.md` (Kịch bản 1) | 3h |
| 5.2 | Chạy test → Kiểm tra kết quả: 5 OK, 495 rejected, balance=0 | `11-testing-plan.md` | 2h |
| 5.3 | Viết kịch bản JMeter: Chuyển tiền chéo A↔B | `11-testing-plan.md` (Kịch bản 2) | 2h |
| 5.4 | Chạy test → Kiểm tra 0 Deadlock, tổng bảo toàn | `11-testing-plan.md` | 2h |
| 5.5 | Test Idempotency Tampering (cùng key, khác payload) | `11-testing-plan.md` (Kịch bản 3) | 1h |
| 5.6 | Integration Test: Full flow Register → Transfer → QR Pay | `11-testing-plan.md` | 3h |
| 5.7 | Test Android trên 2-3 thiết bị thật khác nhau | — | 2h |
| 5.8 | Fix bugs phát sinh từ testing | — | 4h |
| 5.9 | Tối ưu hiệu năng (nếu cần): Index, Query, Pool size | `02-architecture-plan.md` | 2h |

### Tiêu chí nghiệm thu:
- [ ] Kịch bản 1: Đúng 5 TX thành công, 495 bị reject, balance=0, 0 lỗi 500.
- [ ] Kịch bản 2: 0 Deadlock, tổng tiền A+B bảo toàn.
- [ ] Kịch bản 3: Cùng key khác payload → HTTP 422.
- [ ] Tất cả luồng hoạt động end-to-end: Mobile → API → DB → Redis.

---

## GIAI ĐOẠN 6: DEPLOY, HOÀN THIỆN & BÁO CÁO (Tuần 6)

### Mục tiêu:
Deploy production, hoàn thiện tài liệu, viết báo cáo đồ án.

### Nhiệm vụ chi tiết:

| # | Nhiệm vụ | File Plan tham chiếu | Thời gian |
| :--- | :--- | :--- | :--- |
| 6.1 | Viết Dockerfile cho Backend | `12-deployment-plan.md` | 1h |
| 6.2 | Deploy Backend lên Render | `12-deployment-plan.md` | 2h |
| 6.3 | Deploy Frontend lên Vercel | `12-deployment-plan.md` | 1h |
| 6.4 | Build APK Release (signed) | `12-deployment-plan.md` | 1h |
| 6.5 | Cấu hình CI/CD GitHub Actions (Monorepo pipeline với path filter) | `12-deployment-plan.md` | 3h |
| 6.6 | Kiểm tra toàn bộ hệ thống trên production | — | 2h |
| 6.7 | Xuất tài liệu Swagger/OpenAPI | `09-api-contract.md` | 1h |
| 6.8 | Xuất báo cáo JMeter (biểu đồ, p95/p99 latency) | `11-testing-plan.md` | 2h |
| 6.9 | Viết báo cáo đồ án: kiến trúc, sequence diagram, kết quả test | Tất cả files plan | 6h |
| 6.10 | Record demo video ứng dụng | — | 2h |

### Tiêu chí nghiệm thu CUỐI CÙNG:
- [ ] Backend chạy trên Render, URL public truy cập được.
- [ ] Frontend chạy trên Vercel, Admin đăng nhập và thao tác được.
- [ ] APK cài được trên điện thoại thật, mọi chức năng hoạt động.
- [ ] Swagger UI truy cập được trên production.
- [ ] Báo cáo đồ án hoàn thiện với biểu đồ kiến trúc và kết quả JMeter.
- [ ] CI/CD chạy tự động khi push code lên `main`.

---

## BẢNG TỔNG HỢP KHỐI LƯỢNG CÔNG VIỆC

| Giai đoạn | Mô tả | Ước tính |
| :--- | :--- | :--- |
| GĐ 1 | Nền tảng & Hạ tầng | ~10h |
| GĐ 2 | Core Backend Engine | ~34h |
| GĐ 3 | Mobile Android | ~28h |
| GĐ 4 | Web Admin Portal | ~30h |
| GĐ 5 | Kiểm thử & Tối ưu | ~21h |
| GĐ 6 | Deploy & Báo cáo | ~21h |
| **TỔNG** | | **~144h** |
