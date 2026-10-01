'use client';

import React, { useEffect, useState } from 'react';
import { fetchApi } from '@/lib/api';
import { ReconciliationReport } from '@/types/admin';

export default function ReconciliationPage() {
  const [report, setReport] = useState<ReconciliationReport | null>(null);
  const [typeFilter, setTypeFilter] = useState<string>('P2P_TRANSFER');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadReconciliation = async (type: string) => {
    setLoading(true);
    setError(null);
    try {
      const url = type ? `/admin/reconciliation?type=${type}` : '/admin/reconciliation';
      const data = await fetchApi<ReconciliationReport>(url);
      setReport(data);
    } catch (err: unknown) {
      if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('Không thể tải báo cáo đối soát sổ cái');
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadReconciliation(typeFilter);
  }, [typeFilter]);

  const formatVnd = (val: number = 0) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val);
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '28px' }}>
      {/* Title */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <h1 style={{ fontSize: '24px', fontWeight: 800, color: 'var(--text-primary)', letterSpacing: '-0.5px' }}>
            Đối Soát Sổ Cái Kế Toán Kép (ADM-03)
          </h1>
          <p style={{ fontSize: '13px', color: 'var(--text-secondary)', marginTop: '4px' }}>
            Kiểm toán bảo toàn tổng tiền hệ thống (Dual-entry Ledger Audit: Σ DEBIT = Σ CREDIT)
          </p>
        </div>

        {/* Filter Type */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          <label style={{ fontSize: '13px', fontWeight: 600, color: 'var(--text-secondary)' }}>Phân hệ đối soát:</label>
          <select
            className="form-input"
            style={{ width: '220px', padding: '9px 14px' }}
            value={typeFilter}
            onChange={(e) => setTypeFilter(e.target.value)}
          >
            <option value="P2P_TRANSFER">P2P_TRANSFER (Chuyển ví A ↔ B)</option>
            <option value="QR_PAYMENT">QR_PAYMENT (Thanh toán VietQR)</option>
            <option value="TOP_UP">TOP_UP (Nạp tiền vào ví)</option>
            <option value="WITHDRAW">WITHDRAW (Rút tiền khỏi ví)</option>
            <option value="">Toàn bộ giao dịch hệ thống</option>
          </select>
        </div>
      </div>

      {error && (
        <div style={{ padding: '14px 18px', background: 'var(--danger-bg)', border: '1px solid rgba(239, 68, 68, 0.3)', borderRadius: 'var(--radius-md)', color: '#f87171', fontSize: '14px' }}>
          {error}
        </div>
      )}

      {/* Main Status Audit Banner */}
      {report && (
        <div
          className="glass-panel"
          style={{
            padding: '28px',
            background: report.is_balanced
              ? 'linear-gradient(135deg, rgba(16, 185, 129, 0.12) 0%, rgba(16, 23, 38, 0.9) 100%)'
              : 'linear-gradient(135deg, rgba(239, 68, 68, 0.15) 0%, rgba(16, 23, 38, 0.9) 100%)',
            border: report.is_balanced
              ? '1px solid rgba(16, 185, 129, 0.35)'
              : '1px solid rgba(239, 68, 68, 0.4)',
            boxShadow: report.is_balanced ? '0 0 30px rgba(16, 185, 129, 0.15)' : '0 0 30px rgba(239, 68, 68, 0.2)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            flexWrap: 'wrap',
            gap: '20px',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
            <div
              style={{
                width: '54px',
                height: '54px',
                borderRadius: '16px',
                background: report.is_balanced ? 'rgba(16, 185, 129, 0.2)' : 'rgba(239, 68, 68, 0.2)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: report.is_balanced ? '#34d399' : '#f87171',
              }}
            >
              {report.is_balanced ? (
                <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                  <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
                  <polyline points="22 4 12 14.01 9 11.01"></polyline>
                </svg>
              ) : (
                <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                  <polygon points="7.86 2 16.14 2 22 7.86 22 16.14 16.14 22 7.86 22 2 16.14 2 7.86 7.86 2"></polygon>
                  <line x1="12" y1="8" x2="12" y2="12"></line>
                  <line x1="12" y1="16" x2="12.01" y2="16"></line>
                </svg>
              )}
            </div>

            <div>
              <div style={{ fontSize: '18px', fontWeight: 800, color: report.is_balanced ? '#34d399' : '#f87171' }}>
                {report.is_balanced
                  ? 'SỔ CÁI HOÀN TOÀN CÂN BẰNG (ACID COMPLIANT)'
                  : 'CẢNH BÁO: PHÁT HIỆN SAI LỆCH SỔ CÁI'}
              </div>
              <div style={{ fontSize: '13px', color: 'var(--text-secondary)', marginTop: '4px' }}>
                {report.is_balanced
                  ? 'Tổng giá trị nợ (Debit) và giá trị có (Credit) khớp chính xác 100% — Không xảy ra thất thoát tiền tệ.'
                  : 'Chênh lệch phát sinh giữa các bút toán kế toán kép. Cần kiểm toán khẩn cấp.'}
              </div>
            </div>
          </div>

          <div style={{ textAlign: 'right' }}>
            <div style={{ fontSize: '12px', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
              Độ lệch ròng (Net Difference)
            </div>
            <div style={{ fontSize: '24px', fontWeight: 800, color: report.is_balanced ? '#34d399' : '#f87171' }} className="mono">
              {formatVnd(report.net_balance)}
            </div>
          </div>
        </div>
      )}

      {/* Metric Breakdown Grid */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '20px' }}>
        {/* Total Debit Card */}
        <div className="glass-panel" style={{ padding: '24px', borderLeft: '4px solid #ef4444' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '8px' }}>
            <span style={{ fontSize: '13px', fontWeight: 600, color: 'var(--text-secondary)', textTransform: 'uppercase' }}>
              Tổng Nợ (DEBIT - Trừ Tiền)
            </span>
            <span style={{ fontSize: '12px', color: 'var(--text-muted)' }}>
              {report?.debit_entry_count || 0} bút toán
            </span>
          </div>
          <div style={{ fontSize: '26px', fontWeight: 800, color: '#f87171' }} className="mono">
            {formatVnd(report?.total_debit)}
          </div>
          <div style={{ fontSize: '12px', color: 'var(--text-muted)', marginTop: '8px' }}>
            Tổng số tiền được ghi nợ khỏi ví người gửi / nguồn
          </div>
        </div>

        {/* Total Credit Card */}
        <div className="glass-panel" style={{ padding: '24px', borderLeft: '4px solid #10b981' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '8px' }}>
            <span style={{ fontSize: '13px', fontWeight: 600, color: 'var(--text-secondary)', textTransform: 'uppercase' }}>
              Tổng Có (CREDIT - Cộng Tiền)
            </span>
            <span style={{ fontSize: '12px', color: 'var(--text-muted)' }}>
              {report?.credit_entry_count || 0} bút toán
            </span>
          </div>
          <div style={{ fontSize: '26px', fontWeight: 800, color: '#34d399' }} className="mono">
            {formatVnd(report?.total_credit)}
          </div>
          <div style={{ fontSize: '12px', color: 'var(--text-muted)', marginTop: '8px' }}>
            Tổng số tiền được ghi có vào ví người nhận / đích
          </div>
        </div>

        {/* Transaction Count Card */}
        <div className="glass-panel" style={{ padding: '24px', borderLeft: '4px solid #6366f1' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '8px' }}>
            <span style={{ fontSize: '13px', fontWeight: 600, color: 'var(--text-secondary)', textTransform: 'uppercase' }}>
              Số Giao Dịch Kiểm Toán
            </span>
            <span style={{ fontSize: '12px', color: '#818cf8', fontWeight: 600 }}>
              Audit Complete
            </span>
          </div>
          <div style={{ fontSize: '26px', fontWeight: 800, color: 'var(--text-primary)' }}>
            {report?.transaction_count?.toLocaleString('vi-VN') || 0}
          </div>
          <div style={{ fontSize: '12px', color: 'var(--text-muted)', marginTop: '8px' }}>
            Mỗi giao dịch P2P luôn sinh đúng 1 DEBIT + 1 CREDIT song song
          </div>
        </div>
      </div>

      {/* Double Entry Explanation Box */}
      <div className="glass-panel" style={{ padding: '24px' }}>
        <h2 style={{ fontSize: '16px', fontWeight: 700, color: 'var(--text-primary)', marginBottom: '12px' }}>
          Quy Tắc Đối Soát Sổ Cái Kế Toán Kép (Double-entry Ledger)
        </h2>
        <div style={{ fontSize: '13px', color: 'var(--text-secondary)', lineHeight: '1.7', display: 'flex', flexDirection: 'column', gap: '8px' }}>
          <p>
            • Đối với mọi giao dịch chuyển tiền nội bộ (P2P_TRANSFER hoặc QR_PAYMENT giữa 2 ví), hệ thống bắt buộc thực thi 2 bút toán trong cùng 1 Transaction Database:
            <strong style={{ color: 'var(--text-primary)' }}> 1 bút toán DEBIT vào ví nguồn</strong> và
            <strong style={{ color: 'var(--text-primary)' }}> 1 bút toán CREDIT vào ví đích</strong> với cùng số tiền giao dịch.
          </p>
          <p>
            • Điều kiện cân bằng toán học: <code className="mono" style={{ color: '#38bdf8' }}>Σ DEBIT - Σ CREDIT = 0</code>.
          </p>
          <p>
            • Nhờ cơ chế 2-Tier Lock (Redis MultiLock + DB Pessimistic Lock) và Idempotency Hash Protection, hệ thống loại bỏ triệt để hiện tượng sinh tiền ảo hoặc thất thoát số dư.
          </p>
        </div>
      </div>
    </div>
  );
}
