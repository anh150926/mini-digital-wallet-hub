'use client';

import React, { useEffect, useState } from 'react';
import { fetchApi } from '@/lib/api';
import { DashboardStats, TransactionItem, PageResponse } from '@/types/admin';
import StatCard from '@/components/ui/StatCard';
import LineChart from '@/components/charts/LineChart';
import Badge from '@/components/ui/Badge';
import Link from 'next/link';

export default function DashboardPage() {
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [recentTransactions, setRecentTransactions] = useState<TransactionItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadData = async () => {
    setLoading(true);
    setError(null);
    try {
      const [statsData, txData] = await Promise.all([
        fetchApi<DashboardStats>('/admin/dashboard'),
        fetchApi<PageResponse<TransactionItem>>('/admin/transactions?page=0&size=8'),
      ]);
      setStats(statsData);
      setRecentTransactions(txData.content || []);
    } catch (err: unknown) {
      if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('Không thể tải dữ liệu Dashboard');
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const formatVnd = (val: number = 0) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val);
  };

  const formatDate = (isoStr: string) => {
    if (!isoStr) return '-';
    const d = new Date(isoStr);
    return d.toLocaleString('vi-VN', {
      hour: '2-digit',
      minute: '2-digit',
      day: '2-digit',
      month: '2-digit',
    });
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '28px' }}>
      {/* Top Banner */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <div>
          <h1 style={{ fontSize: '24px', fontWeight: 800, color: 'var(--text-primary)', letterSpacing: '-0.5px' }}>
            Tổng Quan Hệ Thống (ADM-01)
          </h1>
          <p style={{ fontSize: '13px', color: 'var(--text-secondary)', marginTop: '4px' }}>
            Giám sát thời gian thực người dùng, ví điện tử và luồng thanh toán VietQR
          </p>
        </div>

        <button onClick={loadData} disabled={loading} className="btn btn-secondary">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <polyline points="23 4 23 10 17 10"></polyline>
            <polyline points="1 20 1 14 7 14"></polyline>
            <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"></path>
          </svg>
          {loading ? 'Đang làm mới...' : 'Làm mới'}
        </button>
      </div>

      {error && (
        <div style={{ padding: '14px 18px', background: 'var(--danger-bg)', border: '1px solid rgba(239, 68, 68, 0.3)', borderRadius: 'var(--radius-md)', color: '#f87171', fontSize: '14px' }}>
          {error}
        </div>
      )}

      {/* 4 Stat Cards */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '20px' }}>
        <StatCard
          title="Tổng Người Dùng"
          value={stats?.total_users?.toLocaleString('vi-VN') || '0'}
          subtitle="Tài khoản trên toàn hệ sinh thái"
          accentColor="#6366f1"
          icon={
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
              <circle cx="9" cy="7" r="4"></circle>
              <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
              <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
            </svg>
          }
        />

        <StatCard
          title="Tổng Ví Đang Hoạt Động"
          value={stats?.total_active_wallets?.toLocaleString('vi-VN') || '0'}
          subtitle="Trạng thái ACTIVE sẵn sàng giao dịch"
          accentColor="#10b981"
          icon={
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <rect x="2" y="4" width="20" height="16" rx="2"></rect>
              <line x1="2" y1="10" x2="22" y2="10"></line>
            </svg>
          }
        />

        <StatCard
          title="Giao Dịch Hôm Nay"
          value={stats?.today_transaction_count?.toLocaleString('vi-VN') || '0'}
          subtitle="Số giao dịch hoàn tất trong ngày"
          accentColor="#06b6d4"
          icon={
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <polyline points="22 12 18 12 15 21 9 3 6 12 2 12"></polyline>
            </svg>
          }
        />

        <StatCard
          title="Dòng Tiền Hôm Nay"
          value={formatVnd(stats?.today_transaction_volume)}
          subtitle="Khối lượng luân chuyển thành công"
          accentColor="#f59e0b"
          icon={
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <line x1="12" y1="1" x2="12" y2="23"></line>
              <path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"></path>
            </svg>
          }
        />
      </div>

      {/* Volume Chart 7 Days */}
      <div className="glass-panel" style={{ padding: '24px' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '20px' }}>
          <div>
            <h2 style={{ fontSize: '16px', fontWeight: 700, color: 'var(--text-primary)' }}>
              Biểu Đồ Dòng Tiền 7 Ngày Gần Nhất
            </h2>
            <p style={{ fontSize: '12px', color: 'var(--text-secondary)', marginTop: '2px' }}>
              Theo dõi biến động dòng tiền thanh toán và chuyển khoản
            </p>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '12px', color: 'var(--text-secondary)' }}>
            <span style={{ width: '10px', height: '10px', borderRadius: '50%', background: '#6366f1', display: 'inline-block' }}></span>
            Khối lượng (VNĐ)
          </div>
        </div>

        <LineChart data={stats?.daily_stats || []} />
      </div>

      {/* Recent Transactions Table */}
      <div className="glass-panel" style={{ padding: '24px' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px' }}>
          <div>
            <h2 style={{ fontSize: '16px', fontWeight: 700, color: 'var(--text-primary)' }}>
              Giao Dịch Gần Nhất
            </h2>
            <p style={{ fontSize: '12px', color: 'var(--text-secondary)', marginTop: '2px' }}>
              8 giao dịch thực hiện gần nhất trên toàn hệ thống
            </p>
          </div>
          <Link href="/transactions" className="btn btn-ghost" style={{ fontSize: '13px' }}>
            Xem tất cả →
          </Link>
        </div>

        <div className="table-wrapper">
          <table className="admin-table">
            <thead>
              <tr>
                <th>Mã Giao Dịch</th>
                <th>Loại</th>
                <th>Số Tiền</th>
                <th>Phí</th>
                <th>Trạng Thái</th>
                <th>Thời Gian</th>
              </tr>
            </thead>
            <tbody>
              {recentTransactions.length === 0 ? (
                <tr>
                  <td colSpan={6} style={{ textAlign: 'center', padding: '36px', color: 'var(--text-muted)' }}>
                    Chưa có giao dịch nào được ghi nhận
                  </td>
                </tr>
              ) : (
                recentTransactions.map((tx) => (
                  <tr key={tx.id}>
                    <td>
                      <div className="mono" style={{ fontSize: '12px', color: '#38bdf8' }}>
                        {tx.idempotency_key ? tx.idempotency_key.substring(0, 18) + '...' : tx.id.substring(0, 13) + '...'}
                      </div>
                      {tx.description && (
                        <div style={{ fontSize: '11px', color: 'var(--text-muted)', marginTop: '2px' }}>
                          {tx.description}
                        </div>
                      )}
                    </td>
                    <td>
                      <span style={{ fontWeight: 600, fontSize: '12px', color: 'var(--text-secondary)' }}>
                        {tx.type}
                      </span>
                    </td>
                    <td style={{ fontWeight: 700, color: 'var(--text-primary)' }}>
                      {formatVnd(tx.amount)}
                    </td>
                    <td style={{ fontSize: '13px', color: 'var(--text-muted)' }}>
                      {formatVnd(tx.fee)}
                    </td>
                    <td>
                      <Badge status={tx.status} />
                    </td>
                    <td style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>
                      {formatDate(tx.created_at)}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
