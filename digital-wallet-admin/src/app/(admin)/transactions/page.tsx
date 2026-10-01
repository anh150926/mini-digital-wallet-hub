'use client';

import React, { useEffect, useState, useCallback } from 'react';
import { fetchApi } from '@/lib/api';
import { TransactionItem, PageResponse } from '@/types/admin';
import Badge from '@/components/ui/Badge';

export default function TransactionsPage() {
  const [transactions, setTransactions] = useState<TransactionItem[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const [typeFilter, setTypeFilter] = useState<string>('');
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadTransactions = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      let url = `/admin/transactions?page=${page}&size=15`;
      if (typeFilter) url += `&type=${typeFilter}`;
      if (statusFilter) url += `&status=${statusFilter}`;

      const data = await fetchApi<PageResponse<TransactionItem>>(url);
      setTransactions(data.content || []);
      setTotalPages(data.total_pages || 1);
      setTotalElements(data.total_elements || 0);
    } catch (err: unknown) {
      if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('Không thể tải lịch sử giao dịch');
      }
    } finally {
      setLoading(false);
    }
  }, [page, typeFilter, statusFilter]);

  useEffect(() => {
    loadTransactions();
  }, [loadTransactions]);

  const handleFilterChange = (newType: string, newStatus: string) => {
    setTypeFilter(newType);
    setStatusFilter(newStatus);
    setPage(0);
  };

  const formatVnd = (val: number = 0) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val);
  };

  const formatDate = (isoStr: string) => {
    if (!isoStr) return '-';
    return new Date(isoStr).toLocaleString('vi-VN');
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      {/* Title */}
      <div>
        <h1 style={{ fontSize: '24px', fontWeight: 800, color: 'var(--text-primary)', letterSpacing: '-0.5px' }}>
          Lịch Sử Giao Dịch Hệ Thống (ADM-03)
        </h1>
        <p style={{ fontSize: '13px', color: 'var(--text-secondary)', marginTop: '4px' }}>
          Truy xuất và lọc bút toán chuyển tiền, thanh toán VietQR trên toàn hệ thống
        </p>
      </div>

      {/* Filter Bar */}
      <div className="glass-panel" style={{ padding: '18px 24px', display: 'flex', alignItems: 'center', gap: '20px', flexWrap: 'wrap' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <label style={{ fontSize: '13px', fontWeight: 600, color: 'var(--text-secondary)' }}>Loại giao dịch:</label>
          <select
            className="form-input"
            style={{ width: '180px', padding: '8px 12px' }}
            value={typeFilter}
            onChange={(e) => handleFilterChange(e.target.value, statusFilter)}
          >
            <option value="">Tất cả loại</option>
            <option value="P2P_TRANSFER">P2P_TRANSFER (Chuyển khoản)</option>
            <option value="QR_PAYMENT">QR_PAYMENT (Thanh toán QR)</option>
            <option value="TOP_UP">TOP_UP (Nạp tiền)</option>
            <option value="WITHDRAW">WITHDRAW (Rút tiền)</option>
          </select>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <label style={{ fontSize: '13px', fontWeight: 600, color: 'var(--text-secondary)' }}>Trạng thái:</label>
          <select
            className="form-input"
            style={{ width: '160px', padding: '8px 12px' }}
            value={statusFilter}
            onChange={(e) => handleFilterChange(typeFilter, e.target.value)}
          >
            <option value="">Tất cả trạng thái</option>
            <option value="SUCCESS">SUCCESS (Thành công)</option>
            <option value="FAILED">FAILED (Thất bại)</option>
            <option value="PENDING">PENDING (Chờ xử lý)</option>
          </select>
        </div>

        {(typeFilter || statusFilter) && (
          <button
            onClick={() => handleFilterChange('', '')}
            className="btn btn-ghost"
            style={{ fontSize: '13px' }}
          >
            Xóa bộ lọc ✕
          </button>
        )}
      </div>

      {error && (
        <div style={{ padding: '14px 18px', background: 'var(--danger-bg)', border: '1px solid rgba(239, 68, 68, 0.3)', borderRadius: 'var(--radius-md)', color: '#f87171', fontSize: '14px' }}>
          {error}
        </div>
      )}

      {/* Transactions Table */}
      <div className="glass-panel" style={{ padding: '20px' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px' }}>
          <div style={{ fontSize: '13px', color: 'var(--text-secondary)' }}>
            Tổng số kết quả: <strong style={{ color: 'var(--text-primary)' }}>{totalElements}</strong> giao dịch
          </div>
        </div>

        <div className="table-wrapper">
          <table className="admin-table">
            <thead>
              <tr>
                <th>Mã Giao Dịch</th>
                <th>Idempotency Key</th>
                <th>Loại</th>
                <th>Số Tiền</th>
                <th>Phí</th>
                <th>Trạng Thái</th>
                <th>Mô Tả</th>
                <th>Thời Gian</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={8} style={{ textAlign: 'center', padding: '40px', color: 'var(--text-muted)' }}>
                    Đang tải dữ liệu giao dịch...
                  </td>
                </tr>
              ) : transactions.length === 0 ? (
                <tr>
                  <td colSpan={8} style={{ textAlign: 'center', padding: '40px', color: 'var(--text-muted)' }}>
                    Không có giao dịch nào thỏa điều kiện lọc
                  </td>
                </tr>
              ) : (
                transactions.map((tx) => (
                  <tr key={tx.id}>
                    <td>
                      <span className="mono" style={{ fontSize: '12px', color: '#38bdf8' }}>
                        {tx.id.substring(0, 13)}...
                      </span>
                    </td>
                    <td>
                      <span className="mono" style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>
                        {tx.idempotency_key ? tx.idempotency_key.substring(0, 16) + '...' : '-'}
                      </span>
                    </td>
                    <td>
                      <span style={{ fontWeight: 600, fontSize: '12px' }}>{tx.type}</span>
                    </td>
                    <td style={{ fontWeight: 700, color: 'var(--text-primary)' }}>
                      {formatVnd(tx.amount)}
                    </td>
                    <td style={{ color: 'var(--text-muted)' }}>
                      {formatVnd(tx.fee)}
                    </td>
                    <td>
                      <Badge status={tx.status} />
                    </td>
                    <td style={{ fontSize: '13px', color: 'var(--text-secondary)', maxWidth: '240px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                      {tx.description || '-'}
                    </td>
                    <td style={{ fontSize: '12px', color: 'var(--text-muted)' }}>
                      {formatDate(tx.created_at)}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginTop: '20px', paddingTop: '16px', borderTop: '1px solid var(--border-color)' }}>
          <div style={{ fontSize: '13px', color: 'var(--text-secondary)' }}>
            Trang {page + 1} / {totalPages}
          </div>
          <div style={{ display: 'flex', gap: '8px' }}>
            <button
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              disabled={page === 0 || loading}
              className="btn btn-secondary"
              style={{ padding: '8px 14px', fontSize: '13px' }}
            >
              ← Trang trước
            </button>
            <button
              onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
              disabled={page >= totalPages - 1 || loading}
              className="btn btn-secondary"
              style={{ padding: '8px 14px', fontSize: '13px' }}
            >
              Trang sau →
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
