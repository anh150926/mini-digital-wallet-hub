# 11 - KẾ HOẠCH KIỂM THỬ & ĐẢM BẢO CHẤT LƯỢNG (TESTING PLAN)

---

## I. CHIẾN LƯỢC KIỂM THỬ (TESTING PYRAMID)

```
                    ┌─────────────┐
                    │   E2E Test  │  ← Ít nhất, chậm nhất, đắt nhất
                    │  (JMeter,   │     Kiểm thử tải 500 threads
                    │   Manual)   │
                    ├─────────────┤
                    │ Integration │  ← Vừa phải
                    │   Tests     │     Test API endpoint + DB + Redis
                    │ (@SpringBoot│
                    │  Test)      │
                    ├─────────────┤
                    │  Unit Tests │  ← Nhiều nhất, nhanh nhất
                    │  (JUnit 5 + │     Test từng service/method riêng lẻ
                    │   Mockito)  │
                    └─────────────┘
```

---

## II. UNIT TESTS

### 2.1. Nguyên tắc viết Unit Test:
- **Mỗi Business Rule (BR-xx) phải có ít nhất 2 test case:** 1 Happy path + 1 Violation path.
- Sử dụng `Mockito` để mock Repository, RedissonClient.
- Test service logic thuần túy, **không kết nối DB/Redis thật**.

### 2.2. Danh sách Unit Test bắt buộc:

#### Module Transaction:

| Test Case | Rule | Kết quả mong đợi |
| :--- | :--- | :--- |
| `transferP2P_success` | TX-01 | Trừ đúng số tiền source, cộng đúng dest |
| `transferP2P_insufficientFunds_throwsException` | BR-GEN-01 | `INSUFFICIENT_FUNDS` |
| `transferP2P_selfTransfer_throwsException` | BR-SPEC-TX02 | `SELF_TRANSFER_NOT_ALLOWED` |
| `transferP2P_belowMinimum_throwsException` | BR-SPEC-TX01 | `BELOW_MINIMUM_AMOUNT` |
| `transferP2P_exceedsPerTxLimit_throwsException` | BR-SPEC-TX01 | `EXCEEDS_PER_TRANSACTION_LIMIT` |
| `transferP2P_exceedsDailyLimit_throwsException` | BR-SPEC-TX01 | `EXCEEDS_DAILY_LIMIT` |
| `transferP2P_walletFrozen_throwsException` | BR-SPEC-TX04 | `WALLET_FROZEN` |
| `transferP2P_createsDebitAndCreditLedger` | BR-GEN-05 | 2 bút toán Debit + Credit |
| `feeCalculation_withdraw_correctAmount` | BR-SPEC-TX03 | Phí = 1100 + 0.1% * amount |
| `feeCalculation_p2pTransfer_zeroFee` | BR-SPEC-TX03 | Phí = 0 |

#### Module QR Code:

| Test Case | Rule | Kết quả mong đợi |
| :--- | :--- | :--- |
| `generateQr_dynamicType_correctTlvPayload` | QR-01 | Payload bắt đầu `000201`, chứa Tag 38 nested |
| `generateQr_crc16Checksum_valid` | QR-01 | 4 ký tự cuối đúng CRC16-CCITT |
| `payQr_expired_throwsException` | BR-SPEC-QR01 | `QR_EXPIRED` |
| `payQr_alreadyUsed_throwsException` | BR-SPEC-QR02 | `QR_ALREADY_USED` |

#### Module Security:

| Test Case | Rule | Kết quả mong đợi |
| :--- | :--- | :--- |
| `verifyPin_correct_resetsFailCounter` | BR-SPEC-SEC01 | Redis counter = 0 |
| `verifyPin_3rdFailure_locks5Minutes` | BR-SPEC-SEC01 | Tạm khóa 5 phút |
| `verifyPin_5thFailure_locks24Hours` | BR-SPEC-SEC01 | Khóa 24h |

#### Module Idempotency:

| Test Case | Rule | Kết quả mong đợi |
| :--- | :--- | :--- |
| `idempotency_newKey_setsProcessing` | BR-GEN-02 | Redis SET PROCESSING |
| `idempotency_completedKey_samePayload_returnsCached` | BR-GEN-02 | HTTP 200 + cached response |
| `idempotency_completedKey_differentPayload_rejects` | BR-GEN-02 | `IDEMPOTENCY_PAYLOAD_MISMATCH` |
| `idempotency_processingKey_rejects` | BR-GEN-02 | HTTP 409 `DUPLICATE_REQUEST` |

---

## III. INTEGRATION TESTS

### 3.1. Cấu hình:
- Sử dụng `@SpringBootTest` với profile `test`.
- Database: Dùng **Testcontainers** (PostgreSQL container) hoặc **H2 in-memory** (mode PostgreSQL).
- Redis: Dùng **Embedded Redis** hoặc **Testcontainers Redis**.

### 3.2. Danh sách Integration Test:

| Test Case | Mô tả |
| :--- | :--- |
| `register_then_login_then_getBalance` | Luồng hoàn chỉnh AUTH-01 → AUTH-02 → WAL-01 |
| `transfer_success_balancesUpdated` | Chuyển tiền thành công, kiểm tra DB balance source & dest |
| `transfer_success_ledgerEntriesCreated` | Kiểm tra 2 bút toán Debit + Credit trong DB |
| `transfer_concurrency_lockOrdering` | 2 threads chuyển tiền chéo A↔B, không deadlock |
| `generateQr_then_payQr_success` | Tạo QR → Quét QR → Thanh toán → is_used=true |
| `adminFreezeWallet_then_transfer_rejected` | Admin khóa ví → User chuyển tiền → 403 WALLET_FROZEN |

---

## IV. LOAD TEST VỚI APACHE JMETER

### 4.1. Kịch bản 1: Rút tiền đồng thời (Anti-Double-Spending)

**Mục tiêu:** Chứng minh hệ thống không cho phép chi tiêu trùng lặp.

| Tham số | Giá trị |
| :--- | :--- |
| **Thread Group** | 500 threads |
| **Ramp-up** | 0 giây (burst đồng thời) |
| **Loop Count** | 1 |
| **Ví nguồn** | 1 ví duy nhất, số dư = 100.000 VNĐ |
| **Mỗi request** | Rút 20.000 VNĐ |
| **Idempotency-Key** | Mỗi thread sinh UUID riêng |

**Kết quả nghiệm thu bắt buộc:**

| Metric | Giá trị bắt buộc |
| :--- | :--- |
| Số TX thành công (HTTP 200) | **Đúng 5** |
| Số TX bị từ chối (HTTP 422) | **Đúng 495** |
| Số lỗi 500 Internal Server Error | **0** |
| Số dư cuối cùng trong DB | **0 VNĐ** |
| Số dư < 0 tại bất kỳ thời điểm nào | **Không bao giờ** |

**Kiểm tra DB sau test:**
```sql
-- Kiểm tra số dư
SELECT balance FROM wallets WHERE id = '<wallet_id>';
-- KẾT QUẢ: 0

-- Kiểm tra số giao dịch thành công
SELECT COUNT(*) FROM transactions
WHERE source_wallet_id = '<wallet_id>' AND status = 'SUCCESS';
-- KẾT QUẢ: 5

-- Kiểm tra đối soát ledger
SELECT
  SUM(CASE WHEN entry_type = 'DEBIT' THEN amount ELSE 0 END) as total_debit,
  SUM(CASE WHEN entry_type = 'CREDIT' THEN amount ELSE 0 END) as total_credit
FROM ledger_entries WHERE wallet_id = '<wallet_id>';
-- total_debit = 100000, total_credit = 0
```

---

### 4.2. Kịch bản 2: Chuyển tiền chéo 2 chiều (Anti-Deadlock)

**Mục tiêu:** Chứng minh Lock Ordering chống Deadlock.

| Tham số | Giá trị |
| :--- | :--- |
| **Thread Group 1** | 200 threads — User A chuyển cho User B (1.000 VNĐ/lần) |
| **Thread Group 2** | 200 threads — User B chuyển cho User A (1.000 VNĐ/lần) |
| **Số dư ban đầu** | A = 1.000.000 VNĐ, B = 1.000.000 VNĐ |

**Kết quả nghiệm thu:**
- **0 lỗi PostgreSQL Deadlock** (error code 40P01).
- **0 lỗi HTTP 500**.
- Tổng số dư (A + B) sau test vẫn = 2.000.000 VNĐ (bảo toàn khối lượng tiền tệ).

---

### 4.3. Kịch bản 3: Idempotency Tampering

**Mục tiêu:** Chứng minh hệ thống chặn payload tampering.

| Bước | Hành động | Kết quả mong đợi |
| :--- | :--- | :--- |
| 1 | Gửi request: Key=K1, amount=50.000 | HTTP 200 OK |
| 2 | Gửi lại request: Key=K1, amount=50.000 (cùng payload) | HTTP 200 OK (cached response, KHÔNG trừ thêm tiền) |
| 3 | Gửi request: Key=K1, amount=500.000 (khác payload) | HTTP 422 `IDEMPOTENCY_PAYLOAD_MISMATCH` |

---

## V. CHECKLIST CHẤT LƯỢNG TRƯỚC KHI MERGE

- [ ] Tất cả Unit Test pass (0 failure).
- [ ] Integration Test pass với DB + Redis thật (hoặc Testcontainers).
- [ ] Không có warning mới trong code (compiler + IDE).
- [ ] API endpoint mới đã được thêm vào Swagger spec.
- [ ] Error code mới đã được thêm vào bảng Error Codes (file `04-business-rules.md`).
