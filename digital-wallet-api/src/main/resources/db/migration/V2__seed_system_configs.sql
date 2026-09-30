-- ============================================================
-- V2: DỮ LIỆU KHỞI TẠO - HẠN MỨC & BIỂU PHÍ MẶC ĐỊNH
-- ============================================================

INSERT INTO system_configs (config_key, config_value, description) VALUES
('TX_MIN_AMOUNT', '10000', 'Số tiền giao dịch tối thiểu (VNĐ)'),
('TX_MAX_PER_TRANSACTION', '5000000', 'Hạn mức tối đa mỗi giao dịch (VNĐ)'),
('TX_MAX_DAILY', '20000000', 'Hạn mức giao dịch tối đa mỗi ngày (VNĐ)'),
('WALLET_MAX_BALANCE', '100000000', 'Số dư tối đa trong ví (VNĐ)'),
('FEE_WITHDRAW_FIXED', '1100', 'Phí rút tiền cố định (VNĐ)'),
('FEE_WITHDRAW_PERCENT', '0.001', 'Phí rút tiền theo phần trăm (0.1%)'),
('FEE_P2P_TRANSFER', '0', 'Phí chuyển tiền nội bộ (VNĐ)'),
('QR_DYNAMIC_EXPIRY_MINUTES', '15', 'Thời gian hết hạn QR động (phút)'),
('PIN_MAX_FAILED_ATTEMPTS', '5', 'Số lần nhập sai PIN tối đa trước khi khóa'),
('PIN_LOCK_DURATION_MINUTES', '15', 'Thời gian tạm khóa khi nhập sai PIN quá giới hạn (phút)');
