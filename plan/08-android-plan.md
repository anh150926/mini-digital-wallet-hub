# 08 - KẾ HOẠCH MOBILE ANDROID (JAVA 17)

---

## I. THÔNG TIN DỰ ÁN

| Thuộc tính | Giá trị |
| :--- | :--- |
| **Language** | Java 17 |
| **Min SDK** | API 26 (Android 8.0 Oreo) |
| **Target/Compile SDK** | API 35 |
| **Architecture** | MVVM (ViewModel + LiveData) |
| **DI** | Manual (Factory Pattern) hoặc Hilt |
| **Navigation** | Jetpack Navigation Component |
| **ViewBinding** | Enabled |
| **Networking** | Retrofit 2 + OkHttp 4 |
| **Camera** | CameraX 1.3+ |
| **QR Scanning** | Google ML Kit Barcode Scanning |
| **Biometric** | AndroidX Biometric 1.2 |
| **Secure Storage** | EncryptedSharedPreferences |
| **Crypto** | Android Keystore (ECDSA P-256) |

---

## II. PACKAGE STRUCTURE

```
com.walletapp.android/
│
├── WalletApplication.java              ← Application class (khởi tạo singletons)
│
├── data/
│   ├── remote/
│   │   ├── ApiService.java             ← Retrofit interface (tất cả endpoints)
│   │   ├── ApiClient.java              ← Singleton Retrofit builder + OkHttp
│   │   ├── AuthInterceptor.java        ← OkHttp Interceptor gắn JWT vào Header
│   │   ├── TokenRefreshAuthenticator.java ← OkHttp Authenticator: tự refresh token khi 401
│   │   └── dto/                        ← Request/Response DTOs
│   │       ├── LoginRequest.java
│   │       ├── LoginResponse.java
│   │       ├── TransferRequest.java
│   │       ├── TransferResponse.java
│   │       ├── QrCodeRequest.java
│   │       ├── QrCodeResponse.java
│   │       ├── WalletResponse.java
│   │       └── ApiErrorResponse.java
│   ├── local/
│   │   └── SecureStorage.java          ← EncryptedSharedPreferences wrapper
│   └── repository/
│       ├── AuthRepository.java         ← Login, Register, Refresh Token
│       ├── WalletRepository.java       ← Get balance, Transaction history
│       ├── TransferRepository.java     ← P2P Transfer
│       └── QrRepository.java           ← Create QR, Pay QR
│
├── domain/
│   ├── model/
│   │   ├── User.java
│   │   ├── Wallet.java
│   │   ├── Transaction.java
│   │   └── QrCodeInfo.java            ← Kết quả parse TLV từ chuỗi QR
│   └── usecase/                        ← (Optional) Clean Architecture use cases
│
├── ui/
│   ├── auth/
│   │   ├── LoginActivity.java          ← Màn hình đăng nhập
│   │   ├── LoginViewModel.java
│   │   ├── RegisterActivity.java       ← Màn hình đăng ký
│   │   └── RegisterViewModel.java
│   │
│   ├── main/
│   │   ├── MainActivity.java           ← Host Activity (chứa NavHostFragment)
│   │   └── MainViewModel.java
│   │
│   ├── home/
│   │   ├── HomeFragment.java           ← Dashboard: Số dư, Quick actions
│   │   └── HomeViewModel.java
│   │
│   ├── transfer/
│   │   ├── TransferFragment.java       ← Form chuyển tiền (SĐT, số tiền, mô tả)
│   │   ├── TransferConfirmFragment.java ← Xác nhận + Quét vân tay
│   │   ├── TransferResultFragment.java  ← Kết quả (Thành công / Thất bại)
│   │   └── TransferViewModel.java
│   │
│   ├── qrscanner/
│   │   ├── QrScannerFragment.java      ← Camera preview + ML Kit scanner
│   │   ├── QrResultFragment.java       ← Hiển thị thông tin người nhận, xác nhận thanh toán
│   │   └── QrScannerViewModel.java
│   │
│   ├── qrgenerate/
│   │   ├── QrGenerateFragment.java     ← Nhập số tiền → Hiển thị QR code
│   │   └── QrGenerateViewModel.java
│   │
│   ├── history/
│   │   ├── HistoryFragment.java        ← Lịch sử giao dịch (RecyclerView)
│   │   ├── TransactionAdapter.java     ← RecyclerView Adapter
│   │   └── HistoryViewModel.java
│   │
│   └── common/
│       ├── LoadingDialog.java          ← Dialog loading toàn màn hình
│       └── ErrorDialog.java            ← Dialog hiển thị lỗi
│
├── security/
│   ├── KeystoreManager.java            ← Sinh/Quản lý ECDSA KeyPair trong Hardware Keystore
│   ├── BiometricHelper.java            ← Wrapper BiometricPrompt + CryptoObject
│   └── TransactionSigner.java          ← Ký payload giao dịch bằng Private Key
│
└── util/
    ├── VietQrParser.java               ← Parse chuỗi TLV → QrCodeInfo object
    ├── Crc16Calculator.java            ← Tính CRC16-CCITT checksum
    ├── NetworkUtils.java               ← Kiểm tra kết nối mạng
    └── Constants.java                  ← BASE_URL, SharedPrefs keys, Request codes
```

---

## III. NAVIGATION GRAPH

```
LoginActivity ──(Đăng nhập thành công)──> MainActivity
                                              │
                                     NavHostFragment
                                              │
                              ┌───────────────┼───────────────┐
                              │               │               │
                        HomeFragment    HistoryFragment   (Settings)
                              │
                    ┌─────────┼─────────┐
                    │         │         │
              TransferFrag  QrScanFrag  QrGenFrag
                    │         │
              ConfirmFrag   QrResultFrag
                    │         │
              ResultFrag    (→ ConfirmFrag → ResultFrag)
```

---

## IV. MÔ TẢ MÀN HÌNH CHI TIẾT

### 4.1. HomeFragment (Dashboard)
- **Hiển thị:** Số dư ví (lớn, nổi bật), Tên chủ ví.
- **Quick Actions (Grid 2x2):**
  - Chuyển tiền → Navigate `TransferFragment`
  - Nhận tiền (Tạo QR) → Navigate `QrGenerateFragment`
  - Quét QR thanh toán → Navigate `QrScannerFragment`
  - Lịch sử → Navigate `HistoryFragment`
- **Data:** Gọi `GET /api/v1/wallets/me` khi Fragment `onResume`.

### 4.2. TransferFragment → ConfirmFragment → ResultFragment
- **TransferFragment:** Form nhập SĐT đích, số tiền, mô tả. Nút "Tiếp tục".
- **ConfirmFragment:**
  - Hiển thị tóm tắt: Người nhận (tên + SĐT), Số tiền, Phí, Tổng trừ.
  - Nút "Xác nhận & Thanh toán" → Trigger BiometricPrompt.
  - Sau khi vân tay thành công → `KeystoreManager` ký payload → Gọi API `POST /transfers`.
- **ResultFragment:** Hiển thị trạng thái SUCCESS/FAILED, mã giao dịch, thời gian.

### 4.3. QrScannerFragment
- **Camera Preview:** Sử dụng `PreviewView` (CameraX).
- **ImageAnalysis:** `STRATEGY_KEEP_ONLY_LATEST`, chỉ quét `FORMAT_QR_CODE`.
- **Luồng xử lý:**
  1. ML Kit detect barcode → Nhận chuỗi raw.
  2. Kiểm tra prefix `000201` (VietQR).
  3. `VietQrParser.parse(rawString)` → `QrCodeInfo` (walletId, amount, description).
  4. Tính lại CRC16, so khớp với 4 ký tự cuối.
  5. Nếu hợp lệ → Dừng camera → Navigate `QrResultFragment` (hiển thị thông tin người nhận).

### 4.4. QrGenerateFragment
- **Form:** Nhập số tiền (tuỳ chọn), mô tả.
- **Gọi API:** `POST /api/v1/qr-codes` → Nhận `payload` (chuỗi TLV).
- **Hiển thị:** Encode `payload` thành QR bitmap (dùng thư viện ZXing).
- **Hẹn giờ:** Hiển thị countdown 15 phút. Khi hết hạn → Disable QR + Nút "Tạo mã mới".

---

## V. BẢO MẬT ANDROID

### 5.1. Lưu trữ an toàn (EncryptedSharedPreferences)

```java
// SecureStorage.java
public class SecureStorage {
    private SharedPreferences prefs;

    public SecureStorage(Context context) {
        this.prefs = EncryptedSharedPreferences.create(
            "wallet_secure_prefs",
            MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );
    }

    public void saveAccessToken(String token) { prefs.edit().putString("access_token", token).apply(); }
    public String getAccessToken() { return prefs.getString("access_token", null); }
    // ... tương tự cho refreshToken, deviceId
}
```

### 5.2. ECDSA KeyPair trong Android Keystore

```java
// KeystoreManager.java — Sinh key lần đầu, key không bao giờ rời khỏi chip bảo mật
public KeyPair generateSigningKey() {
    KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(
            "wallet_tx_key", KeyProperties.PURPOSE_SIGN)
        .setAlgorithmParameterSpec(new ECGenParameterSpec("secp256r1"))
        .setDigests(KeyProperties.DIGEST_SHA256)
        .setUserAuthenticationRequired(true)          // Bắt buộc quét vân tay
        .setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
        .build();

    KeyPairGenerator gen = KeyPairGenerator.getInstance("EC", "AndroidKeyStore");
    gen.initialize(spec);
    return gen.generateKeyPair();
}
```

### 5.3. Kết nối Backend từ Emulator / Thiết bị thật

| Môi trường | BASE_URL | Ghi chú |
| :--- | :--- | :--- |
| Android Emulator | `http://10.0.2.2:8080/api/v1` | `10.0.2.2` = localhost của máy host |
| Thiết bị thật (USB) | `http://localhost:8080/api/v1` | Cần chạy `adb reverse tcp:8080 tcp:8080` |
| Production | `https://api.your-domain.com/api/v1` | HTTPS bắt buộc |
