'use client';

import React, { useEffect, useState, use } from 'react';
import Link from 'next/link';
import { fetchApi } from '@/lib/api';
import { UserDetail } from '@/types/admin';
import Badge from '@/components/ui/Badge';
import Modal from '@/components/ui/Modal';

export default function UserDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const resolvedParams = use(params);
  const userId = resolvedParams.id;

  const [user, setUser] = useState<UserDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Freeze Modal State
  const [modalAction, setModalAction] = useState<'FREEZE' | 'UNFREEZE' | null>(null);
  const [reason, setReason] = useState('Nghi ngờ giao dịch bất thường');
  const [actionLoading, setActionLoading] = useState(false);

  const loadUserDetail = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await fetchApi<UserDetail>(`/admin/users/${userId}`);
      setUser(data);
    } catch (err: unknown) {
      if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('Không thể tải thông tin chi tiết người dùng');
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadUserDetail();
  }, [userId]);

  const handleActionConfirm = async () => {
    if (!user?.wallet_id) return;
    setActionLoading(true);
    try {
      if (modalAction === 'FREEZE') {
        await fetchApi(`/admin/wallets/${user.wallet_id}/freeze`, {
          method: 'PUT',
          body: JSON.stringify({ reason }),
        });
      } else {
        await fetchApi(`/admin/wallets/${user.wallet_id}/unfreeze`, {
          method: 'PUT',
        });
      }
      setModalAction(null);
      loadUserDetail();
    } catch (err: unknown) {
      alert(err instanceof Error ? err.message : 'Thao tác không thành công');
    } finally {
      setActionLoading(false);
    }
  };

  const formatVnd = (val: number = 0) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val);
  };

  const formatDate = (isoStr: string) => {
    if (!isoStr) return '-';
    return new Date(isoStr).toLocaleString('vi-VN');
  };

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: '60px', color: 'var(--text-muted)' }}>
        Đang tải thông tin chi tiết người dùng...
      </div>
    );
  }

  if (error || !user) {
    return (
      <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
        <div style={{ padding: '16px', background: 'var(--danger-bg)', color: '#f87171', borderRadius: 'var(--radius-md)' }}>
          {error || 'Không tìm thấy người dùng'}
        </div>
        <Link href="/users" className="btn btn-secondary" style={{ width: 'fit-content' }}>
          ← Quay lại danh sách
        </Link>
      </div>
    );
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      {/* Back button & Title */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
          <Link href="/users" className="btn btn-secondary" style={{ padding: '8px 14px', fontSize: '13px' }}>
            ← Quay lại
          </Link>
          <div>
            <h1 style={{ fontSize: '22px', fontWeight: 800, color: 'var(--text-primary)' }}>
              {user.full_name}
            </h1>
            <p style={{ fontSize: '13px', color: 'var(--text-secondary)' }} className="mono">
              User ID: {user.user_id}
            </p>
          </div>
        </div>

        {/* Action Button */}
        <div>
          {user.wallet_status === 'ACTIVE' ? (
            <button onClick={() => setModalAction('FREEZE')} className="btn btn-danger">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
                <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
              </svg>
              Đóng Băng Ví
            </button>
          ) : (
            <button onClick={() => setModalAction('UNFREEZE')} className="btn btn-success">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
                <path d="M7 11V7a5 5 0 0 1 9.9-1"></path>
              </svg>
              Mở Khóa Ví
            </button>
          )}
        </div>
      </div>

      {/* Info Grid (Profile & Wallet Cards) */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '20px' }}>
        {/* User Card */}
        <div className="glass-panel" style={{ padding: '24px' }}>
          <h2 style={{ fontSize: '15px', fontWeight: 700, color: '#38bdf8', marginBottom: '16px', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
            Hồ Sơ Người Dùng
          </h2>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', fontSize: '14px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px solid rgba(255,255,255,0.05)', paddingBottom: '8px' }}>
              <span style={{ color: 'var(--text-secondary)' }}>Số điện thoại:</span>
              <span className="mono" style={{ fontWeight: 600 }}>{user.phone_number}</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px solid rgba(255,255,255,0.05)', paddingBottom: '8px' }}>
              <span style={{ color: 'var(--text-secondary)' }}>Họ và tên:</span>
              <span style={{ fontWeight: 600 }}>{user.full_name}</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px solid rgba(255,255,255,0.05)', paddingBottom: '8px' }}>
              <span style={{ color: 'var(--text-secondary)' }}>Vai trò:</span>
              <span style={{ fontWeight: 600 }}>{user.role}</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px solid rgba(255,255,255,0.05)', paddingBottom: '8px' }}>
              <span style={{ color: 'var(--text-secondary)' }}>Trạng thái tài khoản:</span>
              <Badge status={user.user_status} />
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
              <span style={{ color: 'var(--text-secondary)' }}>Ngày tạo:</span>
              <span>{formatDate(user.created_at)}</span>
            </div>
          </div>
        </div>

        {/* Wallet Card */}
        <div className="glass-panel" style={{ padding: '24px', background: 'radial-gradient(ellipse at top right, rgba(79, 70, 229, 0.15) 0%, rgba(16, 23, 38, 0.9) 70%)' }}>
          <h2 style={{ fontSize: '15px', fontWeight: 700, color: '#10b981', marginBottom: '16px', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
            Ví Điện Tử Liên Kết
          </h2>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', fontSize: '14px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px solid rgba(255,255,255,0.05)', paddingBottom: '8px' }}>
              <span style={{ color: 'var(--text-secondary)' }}>Mã Ví:</span>
              <span className="mono" style={{ fontSize: '12px', color: '#38bdf8' }}>{user.wallet_id || 'Chưa khởi tạo'}</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px solid rgba(255,255,255,0.05)', paddingBottom: '8px' }}>
              <span style={{ color: 'var(--text-secondary)' }}>Số dư khả dụng:</span>
              <span style={{ fontSize: '20px', fontWeight: 800, color: 'var(--text-primary)' }}>{formatVnd(user.balance)}</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px solid rgba(255,255,255,0.05)', paddingBottom: '8px' }}>
              <span style={{ color: 'var(--text-secondary)' }}>Trạng thái ví:</span>
              <Badge status={user.wallet_status} />
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
              <span style={{ color: 'var(--text-secondary)' }}>Tiền tệ:</span>
              <span style={{ fontWeight: 600 }}>VND (Việt Nam Đồng)</span>
            </div>
          </div>
        </div>
      </div>

      {/* User's Recent Transactions Table */}
      <div className="glass-panel" style={{ padding: '24px' }}>
        <h2 style={{ fontSize: '16px', fontWeight: 700, color: 'var(--text-primary)', marginBottom: '16px' }}>
          Lịch Sử Giao Dịch Gần Đây Của Ví
        </h2>
        <div className="table-wrapper">
          <table className="admin-table">
            <thead>
              <tr>
                <th>Mã Giao Dịch</th>
                <th>Loại</th>
                <th>Số Tiền</th>
                <th>Phí</th>
                <th>Trạng Thái</th>
                <th>Mô Tả</th>
                <th>Thời Gian</th>
              </tr>
            </thead>
            <tbody>
              {user.recent_transactions.length === 0 ? (
                <tr>
                  <td colSpan={7} style={{ textAlign: 'center', padding: '36px', color: 'var(--text-muted)' }}>
                    Ví này chưa có giao dịch nào
                  </td>
                </tr>
              ) : (
                user.recent_transactions.map((tx) => (
                  <tr key={tx.id}>
                    <td>
                      <span className="mono" style={{ fontSize: '12px', color: '#38bdf8' }}>
                        {tx.idempotency_key ? tx.idempotency_key.substring(0, 18) + '...' : tx.id.substring(0, 13) + '...'}
                      </span>
                    </td>
                    <td><span style={{ fontWeight: 600 }}>{tx.type}</span></td>
                    <td style={{ fontWeight: 700 }}>{formatVnd(tx.amount)}</td>
                    <td style={{ color: 'var(--text-muted)' }}>{formatVnd(tx.fee)}</td>
                    <td><Badge status={tx.status} /></td>
                    <td style={{ fontSize: '13px', color: 'var(--text-secondary)' }}>{tx.description || '-'}</td>
                    <td style={{ fontSize: '12px', color: 'var(--text-muted)' }}>{formatDate(tx.created_at)}</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Confirmation Modal */}
      <Modal
        isOpen={modalAction !== null}
        onClose={() => setModalAction(null)}
        title={modalAction === 'FREEZE' ? 'Xác nhận Đóng Băng Ví' : 'Xác nhận Mở Khóa Ví'}
      >
        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          <p style={{ fontSize: '14px', color: 'var(--text-secondary)' }}>
            {modalAction === 'FREEZE' ? (
              <>Bạn có chắc chắn muốn đóng băng ví của <strong>{user.full_name}</strong>?</>
            ) : (
              <>Bạn có chắc chắn muốn mở khóa ví cho <strong>{user.full_name}</strong>?</>
            )}
          </p>

          {modalAction === 'FREEZE' && (
            <div className="form-group">
              <label className="form-label">Lý do khóa ví:</label>
              <textarea
                className="form-input"
                rows={3}
                value={reason}
                onChange={(e) => setReason(e.target.value)}
              />
            </div>
          )}

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px' }}>
            <button onClick={() => setModalAction(null)} className="btn btn-secondary">Hủy</button>
            <button
              onClick={handleActionConfirm}
              className={modalAction === 'FREEZE' ? 'btn btn-danger' : 'btn btn-success'}
              disabled={actionLoading}
            >
              {actionLoading ? 'Đang xử lý...' : 'Xác nhận'}
            </button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
