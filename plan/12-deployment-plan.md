# 12 - QUY TRÌNH DEPLOY & CI/CD PIPELINE (MONOREPO)

---

## I. TỔNG QUAN DEPLOYMENT ARCHITECTURE

Trong mô hình **Monorepo**, toàn bộ hệ thống nằm trong 1 repo GitHub duy nhất (`mini-digital-wallet-hub`). Mỗi dịch vụ cloud sẽ kết nối vào repo này và cấu hình **Root Directory** riêng:

```text
┌────────────────────────────────────────────────────────────────────────┐
│                        DEVELOPER LOCAL MACHINE                         │
│                                                                        │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────────┐  │
│  │ Backend API      │  │ Frontend Admin   │  │ Mobile Client        │  │
│  │ (Spring Boot)    │  │ (Next.js)        │  │ (Android Studio)     │  │
│  │ localhost:8080   │  │ localhost:3000   │  │ Emulator (API 26-35) │  │
│  └────────┬─────────┘  └────────┬─────────┘  └──────────┬───────────┘  │
│           │                     │                       │              │
└───────────┼─────────────────────┼───────────────────────┼──────────────┘
            │                     │                       │
            └─────────────────────┼───────────────────────┘
                                  ▼
                    git push (1 REPO DUY NHẤT)
                                  │
                                  ▼
           ┌──────────────────────────────────────────────┐
           │ GitHub Monorepo: mini-digital-wallet-hub     │
           │ (Nhánh: main, develop, feat/*)               │
           └──────────────────────┬───────────────────────┘
                                  │
        ┌─────────────────────────┼─────────────────────────┐
        ▼ (Path: api/**)          ▼ (Path: admin/**)        ▼ (Path: android/**)
┌───────────────┐         ┌───────────────┐         ┌────────────────┐
│ GitHub Action │         │ GitHub Action │         │ GitHub Action  │
│ Backend CI    │         │ Frontend CI   │         │ Android CI     │
└───────┬───────┘         └───────┬───────┘         └────────┬───────┘
        │                         │                          │
        ▼                         ▼                          ▼
┌───────────────┐         ┌───────────────┐         ┌────────────────┐
│ Render Cloud  │         │ Vercel Cloud  │         │ GitHub Release │
│ (Root Dir:    │         │ (Root Dir:    │         │ (Phân phối file│
│  wallet-api)  │         │  wallet-admin)│         │  app-release   │
│               │         │               │         │  .apk)         │
└───────┬───────┘         └───────┬───────┘         └────────────────┘
        │                         │
        ▼                         ▼
┌─────────────────────────────────────────────────────────┐
│ Cloud Database & Cache Infrastructure                   │
│ ┌──────────────────────────┐  ┌───────────────────────┐ │
│ │ Neon.tech PostgreSQL 16  │  │ Upstash Redis 7 (TLS) │ │
│ └──────────────────────────┘  └───────────────────────┘ │
└─────────────────────────────────────────────────────────┘
```

---

## II. NỀN TẢNG DEPLOY KHUYẾN NGHỊ (HOÀN TOÀN MIỄN PHÍ)

| Thành phần | Nền tảng | Lý do lựa chọn | Chi phí |
| :--- | :--- | :--- | :--- |
| **Backend API** | **Render.com** | Hỗ trợ Dockerfile, tự build Maven, SSL miễn phí, tương thích Monorepo | $0 / tháng |
| **Frontend Web** | **Vercel** | Tối ưu 100% cho Next.js, CDN toàn cầu, auto-detect root directory | $0 / tháng |
| **Database** | **Neon.tech** | PostgreSQL 16 serverless, connection pooling, SSL bắt buộc | $0 / tháng |
| **Cache & Lock** | **Upstash** | Serverless Redis có TLS, hỗ trợ Distributed Lock Redisson | $0 / tháng |
| **Mobile App** | **GitHub Releases** | Lưu trữ và tải file APK trực tiếp từ trang GitHub repo | $0 / tháng |

---

## III. QUY TRÌNH DEPLOY CHI TIẾT TỪNG THÀNH PHẦN

### 3.1. Deploy Backend lên Render (từ Monorepo)

#### Bước 1: Tạo `Dockerfile` trong thư mục `digital-wallet-api/`
```dockerfile
# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B

# Stage 2: Run
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### Bước 2: Cấu hình trên Render Dashboard
1. Truy cập [render.com](https://render.com) $\rightarrow$ Đăng nhập bằng GitHub.
2. Bấm **New +** $\rightarrow$ Chọn **Web Service**.
3. Chọn repo GitHub: **`mini-digital-wallet-hub`**.
4. Cấu hình quan trọng cho Monorepo:
   * **Name**: `digital-wallet-api`
   * **Region**: `Singapore`
   * **Root Directory**: 👉 **`digital-wallet-api`** *(Bắt buộc điền mục này để Render chỉ build backend)*
   * **Runtime**: `Docker`
5. Nhập các **Environment Variables**:
   ```env
   DATABASE_URL=jdbc:postgresql://ep-falling-grass-b317h7w6-pooler.c-4.ap-southeast-1.aws.neon.tech/neondb?sslmode=require
   DATABASE_USERNAME=neondb_owner
   DATABASE_PASSWORD=npg_rD2Tnzxoft6J
   REDIS_HOST=live-yeti-319474.upstash.io
   REDIS_PORT=6379
   REDIS_PASSWORD=gQAAAAAABN_yAAIgcDE0MmU1MTExNTA5Yzk0MjExOWY3YWUxNTgyZjQ0YzUwYw
   REDIS_SSL=true
   JWT_SECRET=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
   APP_ENV=prod
   ```
6. Bấm **Create Web Service** $\rightarrow$ Nhận URL công khai dạng: `https://digital-wallet-api.onrender.com`.

---

### 3.2. Deploy Frontend lên Vercel (từ Monorepo)

1. Truy cập [vercel.com](https://vercel.com) $\rightarrow$ Đăng nhập bằng GitHub.
2. Bấm **Add New...** $\rightarrow$ **Project**.
3. Chọn repo: **`mini-digital-wallet-hub`**.
4. Cấu hình Monorepo:
   * Ở dòng **Root Directory**: bấm nút **Edit** $\rightarrow$ chọn thư mục 👉 **`digital-wallet-admin`**.
   * Framework Preset: Vercel sẽ tự động nhận diện `Next.js`.
5. Thêm **Environment Variables**:
   * Key: `NEXT_PUBLIC_API_URL`
   * Value: `https://digital-wallet-api.onrender.com/api/v1`
6. Bấm **Deploy** $\rightarrow$ Nhận URL trang quản trị: `https://digital-wallet-admin.vercel.app`.

---

### 3.3. Đóng gói & Phát hành APK Android

```powershell
# 1. Di chuyển vào thư mục Android
cd "D:\Mini Digital Wallet & QR Payment Hub\digital-wallet-android"

# 2. Build file APK Release
./gradlew assembleRelease

# 3. File APK tạo ra tại:
# app/build/outputs/apk/release/app-release.apk

# 4. Đưa lên GitHub Releases để người dùng tải về:
git tag v1.0.0
git push origin v1.0.0
# Vào GitHub -> Releases -> Draft a new release -> Kéo thả file .apk vào đính kèm
```

---

## IV. CI/CD PIPELINE CHO MONOREPO (GITHUB ACTIONS)

Điểm mạnh của Monorepo là ta có thể dùng tính năng **Path Filtering** để chỉ kích hoạt luồng build cho thư mục có sự thay đổi code:

### 4.1. Backend CI (`.github/workflows/backend-ci.yml`)

```yaml
name: Backend CI (Spring Boot)

on:
  push:
    paths:
      - 'digital-wallet-api/**'
      - '.github/workflows/backend-ci.yml'
    branches: [main, develop]
  pull_request:
    paths:
      - 'digital-wallet-api/**'
    branches: [main, develop]

jobs:
  build:
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: ./digital-wallet-api

    steps:
      - uses: actions/checkout@v4
      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
      - name: Cache Maven
        uses: actions/cache@v4
        with:
          path: ~/.m2/repository
          key: ${{ runner.os }}-maven-${{ hashFiles('digital-wallet-api/pom.xml') }}
      - name: Run Tests & Build JAR
        run: mvn clean package -B
```

### 4.2. Frontend CI (`.github/workflows/frontend-ci.yml`)

```yaml
name: Frontend CI (Next.js)

on:
  push:
    paths:
      - 'digital-wallet-admin/**'
      - '.github/workflows/frontend-ci.yml'
    branches: [main, develop]
  pull_request:
    paths:
      - 'digital-wallet-admin/**'
    branches: [main, develop]

jobs:
  build:
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: ./digital-wallet-admin

    steps:
      - uses: actions/checkout@v4
      - name: Set up Node.js 20
        uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: 'npm'
          cache-dependency-path: 'digital-wallet-admin/package-lock.json'
      - name: Install & Lint & Build
        run: |
          npm ci
          npm run lint
          npm run build
```
