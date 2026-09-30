# 07 - KẾ HOẠCH FRONTEND NEXT.JS ADMIN PORTAL

---

## I. THÔNG TIN DỰ ÁN

| Thuộc tính | Giá trị |
| :--- | :--- |
| **Framework** | Next.js 14+ (App Router) |
| **Language** | TypeScript |
| **Styling** | Vanilla CSS (CSS Modules) |
| **Auth** | JWT (Access Token in memory, Refresh Token in HttpOnly Cookie) |
| **API Client** | Fetch API (native) |
| **Port (Dev)** | 3000 |

---

## II. KHỞI TẠO DỰ ÁN

```bash
npx create-next-app@latest digital-wallet-admin --typescript --eslint --app --src-dir --no-tailwind --import-alias "@/*"
```

---

## III. ROUTE STRUCTURE (App Router)

```
src/app/
├── layout.tsx                          ← Root layout (font, global CSS)
├── page.tsx                            ← Landing → Redirect to /login
├── globals.css                         ← Design tokens, CSS variables
│
├── (auth)/                             ← Route group: không cần sidebar
│   ├── login/
│   │   └── page.tsx                    ← Trang đăng nhập Admin
│   └── layout.tsx                      ← Auth layout (centered card)
│
├── (admin)/                            ← Route group: có sidebar + header
│   ├── layout.tsx                      ← Admin layout (sidebar, topbar, guard)
│   │
│   ├── dashboard/
│   │   └── page.tsx                    ← ADM-01: Tổng quan (dòng tiền, số TX, biểu đồ)
│   │
│   ├── users/
│   │   ├── page.tsx                    ← ADM-02: Danh sách users + tìm kiếm
│   │   └── [id]/
│   │       └── page.tsx                ← ADM-02: Chi tiết user + ví + nút Khóa/Mở
│   │
│   ├── transactions/
│   │   └── page.tsx                    ← ADM-03: Bảng giao dịch + filter + đối soát
│   │
│   ├── reconciliation/
│   │   └── page.tsx                    ← ADM-03: Trang đối soát sổ cái kế toán kép
│   │
│   └── settings/
│       └── page.tsx                    ← ADM-04: Cấu hình hạn mức, biểu phí
```

---

## IV. COMPONENT TREE

```
src/components/
├── layout/
│   ├── Sidebar.tsx                     ← Menu điều hướng bên trái
│   ├── Topbar.tsx                      ← Header: tên admin, avatar, logout
│   └── Breadcrumb.tsx                  ← Đường dẫn vị trí hiện tại
│
├── ui/
│   ├── Button.tsx                      ← Button tái sử dụng (variant: primary, danger, ghost)
│   ├── Input.tsx                       ← Text input có label + error state
│   ├── Modal.tsx                       ← Dialog xác nhận (VD: "Bạn chắc chắn muốn khóa ví?")
│   ├── Badge.tsx                       ← Status badge (SUCCESS=green, FAILED=red, PENDING=yellow)
│   ├── Card.tsx                        ← Container card (dùng cho Dashboard stats)
│   ├── Table.tsx                       ← Bảng dữ liệu (sortable, paginated)
│   ├── Spinner.tsx                     ← Loading indicator
│   └── EmptyState.tsx                  ← Placeholder khi không có dữ liệu
│
├── charts/
│   └── LineChart.tsx                   ← Biểu đồ dòng tiền theo ngày (dùng Chart.js hoặc Recharts)
│
└── features/
    ├── UserDetail.tsx                  ← Component chi tiết user + wallet info
    ├── TransactionTable.tsx            ← Bảng giao dịch với filter status/type/date
    ├── ReconciliationReport.tsx        ← Báo cáo đối soát Debit vs Credit
    └── ConfigForm.tsx                  ← Form chỉnh sửa system_configs
```

---

## V. API CLIENT LAYER

```
src/lib/
├── api.ts                              ← Base fetch wrapper (attach token, handle errors)
├── auth.ts                             ← Login, logout, refresh token
├── endpoints/
│   ├── dashboard.ts                    ← GET /admin/dashboard
│   ├── users.ts                        ← GET /admin/users, PUT /admin/wallets/{id}/freeze
│   ├── transactions.ts                 ← GET /admin/transactions
│   ├── reconciliation.ts              ← GET /admin/reconciliation
│   └── configs.ts                      ← GET/PUT /admin/configs
└── types/
    ├── user.ts                         ← User, Wallet type definitions
    ├── transaction.ts                  ← Transaction, LedgerEntry types
    └── common.ts                       ← ApiResponse<T>, PageResponse<T>
```

### Mẫu API Client (`src/lib/api.ts`):
```typescript
const API_BASE = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api/v1';

let accessToken: string | null = null;

export function setAccessToken(token: string) { accessToken = token; }
export function clearAccessToken() { accessToken = null; }

export async function apiFetch<T>(
  endpoint: string,
  options: RequestInit = {}
): Promise<T> {
  const headers: HeadersInit = {
    'Content-Type': 'application/json',
    ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
    ...options.headers,
  };

  const res = await fetch(`${API_BASE}${endpoint}`, { ...options, headers });

  if (res.status === 401) {
    // Thử refresh token (gọi /auth/refresh với HttpOnly cookie)
    // Nếu thất bại → redirect về /login
  }

  if (!res.ok) {
    const error = await res.json();
    throw new ApiError(error.error_code, error.message, res.status);
  }

  return res.json();
}
```

---

## VI. TRANG CHÍNH — MÔ TẢ CHI TIẾT

### 6.1. Dashboard (ADM-01)
**Dữ liệu hiển thị:**
- 4 Card thống kê: Tổng Users, Tổng Ví Active, Tổng Giao Dịch Hôm Nay, Tổng Dòng Tiền.
- Biểu đồ đường: Dòng tiền giao dịch theo 7 ngày gần nhất.
- Bảng: 10 giao dịch gần nhất (real-time).

### 6.2. Quản lý Users (ADM-02)
**Chức năng:**
- Bảng danh sách users: SĐT, Tên, Trạng thái, Số dư, Ngày tạo. Có tìm kiếm theo SĐT/Tên.
- Click vào user → Trang chi tiết:
  - Thông tin cá nhân.
  - Thông tin ví (số dư, trạng thái, ngày tạo).
  - Lịch sử giao dịch của user (phân trang).
  - Nút **Khóa ví** (Freeze) / **Mở khóa ví** (Unfreeze) — hiện Modal xác nhận trước khi thực thi.

### 6.3. Đối soát Sổ cái (ADM-03)
**Chức năng:**
- Bảng giao dịch: Lọc theo Status, Type, Khoảng thời gian.
- Báo cáo đối soát: Tổng DEBIT vs Tổng CREDIT cho các giao dịch P2P (phải bằng nhau).
- Cảnh báo đỏ nếu sai lệch.

### 6.4. Cấu hình Hệ thống (ADM-04)
**Chức năng:**
- Form chỉnh sửa từng cặp key-value trong `system_configs`.
- Chỉ `SUPER_ADMIN` và `OWNER` mới được phép chỉnh sửa.
- Lưu lịch sử thay đổi (ai sửa, sửa lúc nào, giá trị cũ → mới).

---

## VII. BẢO MẬT PHÍA FRONTEND

1. **Access Token:** Lưu trong biến JS (memory) — mất khi đóng tab/refresh. Sau khi refresh trang, gọi API `/auth/refresh` (cookie HttpOnly) để lấy token mới.
2. **Refresh Token:** Lưu trong `HttpOnly Secure SameSite=Strict Cookie` — JavaScript không thể đọc, chống XSS.
3. **Admin Guard:** `(admin)/layout.tsx` kiểm tra role trước khi render — nếu không phải ADMIN/SUPER_ADMIN/OWNER → Redirect về /login.
4. **CORS:** Backend chỉ cho phép origin `http://localhost:3000` (dev) và domain production.
