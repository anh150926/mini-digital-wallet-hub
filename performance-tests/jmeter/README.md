# Hướng Dẫn Thực Thi Kiểm Thử Tải Trọng Với Apache JMeter

Thư mục này chứa các kịch bản kiểm thử tải trọng (Performance & Load Testing) cho hệ sinh thái **Mini Digital Wallet & QR Payment Hub**.

---

## I. Danh Sách Kịch Bản Test Plan

1. **`anti_double_spending_500_threads.jmx` (Kịch bản 1):**
   - **Mục tiêu:** Kiểm thử chống chi tiêu trùng lặp (Anti-Double-Spending).
   - **Tải trọng:** 500 threads gửi request rút tiền đồng loạt (burst đồng thời, ramp-up 0s).
   - **Số dư ban đầu:** 100.000 VNĐ.
   - **Mỗi request:** 20.000 VNĐ.
   - **Tiêu chí nghiệm thu:**
     - Đúng 5 request thành công (HTTP 200).
     - 495 request bị từ chối (HTTP 422 hoặc HTTP 409).
     - 0 lỗi hệ thống HTTP 500.
     - Số dư ví cuối cùng bằng đúng 0 VNĐ, không bao giờ âm.

2. **`cross_transfer_anti_deadlock.jmx` (Kịch bản 2):**
   - **Mục tiêu:** Kiểm thử chống bế tắc cơ sở dữ liệu (Anti-Deadlock với 2-tier Lock Ordering: Redis Redisson MultiLock + DB Pessimistic Lock).
   - **Tải trọng:** 200 threads User A chuyển cho User B và 200 threads User B chuyển cho User A đồng thời.
   - **Tiêu chí nghiệm thu:**
     - 0 Deadlock PostgreSQL (mã lỗi 40P01).
     - 0 lỗi HTTP 500.
     - Tổng số dư `Wallet A + Wallet B` sau test được bảo toàn 100%.

---

## II. Hướng Dẫn Chạy Bằng JMeter CLI (Không Cần GUI)

### 1. Yêu cầu tiên quyết:
- Đã cài đặt Apache JMeter (tải từ [jmeter.apache.org](https://jmeter.apache.org/download_jmeter.cgi)).
- Backend `digital-wallet-api` đang chạy tại `http://localhost:8080`.

### 2. Lệnh thực thi Kịch bản 1:
```bash
jmeter -n -t anti_double_spending_500_threads.jmx \
  -JHOST=localhost \
  -JPORT=8080 \
  -JTOKEN_SENDER="<YOUR_ACCESS_TOKEN>" \
  -JRECEIVER_PHONE="0911223344" \
  -l results_scenario1.jtl \
  -e -o ./report_scenario1
```

### 3. Lệnh thực thi Kịch bản 2:
```bash
jmeter -n -t cross_transfer_anti_deadlock.jmx \
  -JHOST=localhost \
  -JPORT=8080 \
  -JTOKEN_A="<TOKEN_USER_A>" \
  -JTOKEN_B="<TOKEN_USER_B>" \
  -JPHONE_A="0901000001" \
  -JPHONE_B="0901000002" \
  -l results_scenario2.jtl \
  -e -o ./report_scenario2
```

Sau khi chạy xong, mở thư mục `./report_scenario1` hoặc `./report_scenario2/index.html` trong trình duyệt để xem biểu đồ p90, p95, p99 latency và tỉ lệ response code.
