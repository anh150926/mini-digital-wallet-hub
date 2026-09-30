# 12 - QUY TRÌNH DEPLOY & CI/CD PIPELINE

---

## I. TỔNG QUAN DEPLOYMENT ARCHITECTURE

```
┌──────────────────────────────────────────────────────────────┐
│                      DEVELOPER MACHINE                        │
│                                                                │
│  ┌─────────────┐  ┌──────────────────┐  ┌─────────────────┐  │
│  │ Backend     │  │ Frontend Admin   │  │ Android Studio  │  │
│  │ (IntelliJ)  │  │ (VS Code)        │  │                 │  │
│  │ localhost:  │  │ localhost:3000    │  │ Emulator/USB    │  │
│  │ 8080        │  │                  │  │                 │  │
│  └──────┬──────┘  └────────┬─────────┘  └────────┬────────┘  │
│         │                  │                     │            │
└─────────┼──────────────────┼─────────────────────┼────────────┘
          │                  │                     │
     git push           git push              Manual Build
          │                  │                     │
          ▼                  ▼                     ▼
┌──────────────┐  ┌──────────────────┐  ┌──────────────────┐
│ GitHub Repo  │  │ GitHub Repo      │  │ GitHub Repo      │
│ wallet-api   │  │ wallet-admin     │  │ wallet-android   │
└──────┬───────┘  └────────┬─────────┘  └────────┬─────────┘
       │                   │                     │
       │ GitHub Actions    │ GitHub Actions       │ GitHub Actions
       │ (Auto CI)         │ (Auto CI)            │ (Auto CI)
       ▼                   ▼                     ▼
┌──────────────┐  ┌──────────────────┐  ┌──────────────────┐
│  Test &      │  │  Build &         │  │  Build APK       │
│  Build JAR   │  │  Export Static   │  │  (Debug/Release) │
└──────┬───────┘  └────────┬─────────┘  └────────┬─────────┘
       │                   │                     │
       ▼                   ▼                     ▼
┌──────────────┐  ┌──────────────────┐  ┌──────────────────┐
│  Railway /   │  │  Vercel          │  │  GitHub Releases │
│  Render /    │  │  (Tự động)       │  │  hoặc Firebase   │
│  Fly.io      │  │                  │  │  App Distribution│
└──────────────┘  └──────────────────┘  └──────────────────┘
       │                   │
       ▼                   ▼
┌──────────────────────────────────────┐
│  Cloud Database & Cache               │
│  ┌──────────────┐  ┌──────────────┐  │
│  │ Neon.tech    │  │ Upstash      │  │
│  │ PostgreSQL   │  │ Redis        │  │
│  └──────────────┘  └──────────────┘  │
└──────────────────────────────────────┘
```

---

## II. NỀN TẢNG DEPLOY KHUYẾN NGHỊ

### 2.1. So sánh nền tảng Deploy Backend (Spring Boot JAR)

| Nền tảng | Free Tier | Docker? | Java Support | Ưu điểm | Nhược điểm |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Railway** | $5 credit/tháng | Có (auto) | Java 21 ✅ | Deploy cực nhanh, auto-detect Dockerfile | Credit có hạn |
| **Render** | 750h/tháng | Có | Java 21 ✅ | Free tier rộng, auto-deploy từ Git | Cold start chậm (spin down sau 15 phút idle) |
| **Fly.io** | 3 shared VMs | Có | Java 21 ✅ | Cấu hình linh hoạt, region châu Á | Cần viết Dockerfile |
| **Koyeb** | 1 nano instance | Có | Java 21 ✅ | Không cold start | RAM ít (256MB free) |

**Khuyến nghị:** **Render** (đơn giản nhất, free tier đủ dùng cho đồ án, không cần Docker Desktop — Render tự build từ Dockerfile trên cloud).

### 2.2. Deploy Frontend Next.js

| Nền tảng | Khuyến nghị | Lý do |
| :--- | :--- | :--- |
| **Vercel** | ⭐ **Tốt nhất** | Vercel là nhà phát triển Next.js — tối ưu hoàn hảo, deploy 1 click từ GitHub |
| Netlify | Tốt | Hỗ trợ Next.js nhưng không tối ưu bằng Vercel |

### 2.3. Phân phối APK Android

| Phương pháp | Khi nào dùng | Cách làm |
| :--- | :--- | :--- |
| **Build APK local** | Debug & test nhanh | Android Studio → Build → Generate Signed APK |
| **GitHub Releases** | Chia sẻ cho team/giáo viên | Upload `.apk` vào Release tag |
| **Firebase App Distribution** | Test beta | Upload lên Firebase Console |
| **Google Play (Internal Testing)** | Production | Tạo tài khoản Developer ($25) |

---

## III. QUY TRÌNH DEPLOY TỪNG THÀNH PHẦN

### 3.1. Deploy Backend lên Render

#### Bước 1: Tạo `Dockerfile` (trong repo `digital-wallet-api`)
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

> **Lưu ý:** Bạn KHÔNG CẦN Docker Desktop trên máy local. Render sẽ tự detect file `Dockerfile` trong repo và build trên cloud server của họ.

#### Bước 2: Cấu hình trên Render Dashboard
1. Đăng ký [render.com](https://render.com) bằng GitHub.
2. New → Web Service → Connect GitHub repo `digital-wallet-api`.
3. Render tự detect `Dockerfile`.
4. Thêm **Environment Variables:**
   ```
   DATABASE_URL=jdbc:postgresql://ep-xxx.neon.tech/neondb?sslmode=require
   DATABASE_USERNAME=neondb_owner
   DATABASE_PASSWORD=<password>
   REDIS_URL=rediss://default:<password>@xxx.upstash.io:6379
   JWT_SECRET=<your-256-bit-secret>
   APP_ENV=prod
   ```
5. Bấm **Deploy** → Render tự build Docker image và chạy.
6. Nhận URL public: `https://digital-wallet-api.onrender.com`.

#### Bước 3: Cấu hình Auto-Deploy
- Render mặc định auto-deploy khi có commit mới trên nhánh `main`.
- Hoặc tắt auto-deploy và dùng manual deploy khi sẵn sàng.

---

### 3.2. Deploy Frontend lên Vercel

#### Bước 1: Kết nối Vercel với GitHub
1. Đăng ký [vercel.com](https://vercel.com) bằng GitHub.
2. Import repo `digital-wallet-admin`.
3. Vercel tự detect Next.js → Auto-config build settings.

#### Bước 2: Thêm Environment Variables
```
NEXT_PUBLIC_API_URL=https://digital-wallet-api.onrender.com/api/v1
```

#### Bước 3: Deploy
- Bấm **Deploy** → Vercel build và deploy.
- Nhận URL: `https://digital-wallet-admin.vercel.app`.
- Mọi commit vào `main` sẽ auto-deploy.
- Mọi PR sẽ tạo **Preview Deployment** (URL riêng để review).

---

### 3.3. Build & Phân phối APK Android

#### Build APK Debug (test nội bộ):
```bash
# Trong Android Studio Terminal
./gradlew assembleDebug

# APK output: app/build/outputs/apk/debug/app-debug.apk
```

#### Build APK Release (chia sẻ/nộp đồ án):
1. Android Studio → Build → Generate Signed Bundle/APK.
2. Chọn APK → Tạo hoặc chọn Keystore file (`.jks`).
3. Build variant: `release`.
4. APK output: `app/build/outputs/apk/release/app-release.apk`.

#### Upload lên GitHub Releases:
1. Tạo Tag: `git tag v1.0.0 && git push --tags`.
2. GitHub → Releases → Create Release → Attach `app-release.apk`.

---

## IV. CI/CD PIPELINE (GITHUB ACTIONS)

### 4.1. Backend CI (`.github/workflows/backend-ci.yml`)

```yaml
name: Backend CI

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main, develop]

jobs:
  test-and-build:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Cache Maven dependencies
        uses: actions/cache@v4
        with:
          path: ~/.m2/repository
          key: ${{ runner.os }}-maven-${{ hashFiles('**/pom.xml') }}

      - name: Run Unit Tests
        run: mvn test -B

      - name: Build JAR (skip tests)
        run: mvn package -DskipTests -B

      - name: Upload JAR artifact
        uses: actions/upload-artifact@v4
        with:
          name: app-jar
          path: target/*.jar
```

### 4.2. Frontend CI (`.github/workflows/frontend-ci.yml`)

```yaml
name: Frontend CI

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main, develop]

jobs:
  lint-and-build:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - name: Set up Node.js 20
        uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: 'npm'

      - name: Install dependencies
        run: npm ci

      - name: Run ESLint
        run: npm run lint

      - name: Build Next.js
        run: npm run build
        env:
          NEXT_PUBLIC_API_URL: https://digital-wallet-api.onrender.com/api/v1
```

### 4.3. Android CI (`.github/workflows/android-ci.yml`)

```yaml
name: Android CI

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main, develop]

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Cache Gradle
        uses: actions/cache@v4
        with:
          path: |
            ~/.gradle/caches
            ~/.gradle/wrapper
          key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*') }}

      - name: Build Debug APK
        run: ./gradlew assembleDebug

      - name: Upload APK
        uses: actions/upload-artifact@v4
        with:
          name: app-debug
          path: app/build/outputs/apk/debug/app-debug.apk
```

---

## V. QUY TRÌNH RELEASE (RELEASE FLOW)

```
1. Phát triển trên nhánh feat/* → Merge vào develop (qua PR)
2. Khi develop ổn định → Tạo nhánh release/v1.0.0
3. Test cuối cùng trên nhánh release
4. Merge release/v1.0.0 → main (qua PR)
5. Tạo Git Tag: v1.0.0
6. Render auto-deploy Backend từ main
7. Vercel auto-deploy Frontend từ main
8. Build APK Release → Upload GitHub Releases
9. Cập nhật CHANGELOG.md
```

---

## VI. CHECKLIST TRƯỚC KHI DEPLOY PRODUCTION

- [ ] Tất cả Unit Tests pass trên CI.
- [ ] Integration Tests pass.
- [ ] Environment Variables đã được set đúng trên Render/Vercel.
- [ ] Database Migration (Flyway) chạy thành công trên Cloud DB.
- [ ] CORS config chỉ cho phép domain production.
- [ ] `spring.jpa.show-sql=false` trong profile `prod`.
- [ ] File `.env` KHÔNG có trong Git repository.
- [ ] APK đã được ký bằng Release Keystore.
- [ ] API Swagger spec đã cập nhật (version mới).
- [ ] Tested trên Android thiết bị thật (không chỉ Emulator).
