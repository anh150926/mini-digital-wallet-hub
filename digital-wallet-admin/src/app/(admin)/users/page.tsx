'use client';

import React, { useEffect, useState, useCallback } from 'react';
import Link from 'next/link';
import { fetchApi } from '@/lib/api';
import { UserSummary, PageResponse } from '@/types/admin';
import Badge from '@/components/ui/Badge';
import Modal from '@/components/ui/Modal';

export default function UsersPage() {
  const [users, setUsers] = useState<UserSummary[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const [search, setSearch] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Modal State
  const [selectedUser, setSelectedUser] = useState<UserSummary | null>(null);
  const [modalAction, setModalAction] = useState<'FREEZE' | 'UNFREEZE' | null>(null);
  const [reason, setReason] = useState('Nghi ngờ giao dịch bất thường');
  const [actionLoading, setActionLoading] = useState(false);

  // Debounce search
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedSearch(search);
      setPage(0);
    }, 400);
    return () => clearTimeout(timer);
  }, [search]);

  const loadUsers = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const queryParam = debouncedSearch ? `&search=${encodeURIComponent(debouncedSearch)}` : '';
      const data = await fetchApi<PageResponse<UserSummary>>(`/admin/users?page=${page}&size=15${queryParam}`);
      setUsers(data.content || []);
      setTotalPages(data.total_pages || 1);
      setTotalElements(data.total_elements || 0);
    } catch (err: unknown) {
      if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('Không thể tải danh sách người dùng');
      }
    } finally {
      setLoading(false);
    }
  }, [page, debouncedSearch]);

  useEffect(() => {
    loadUsers();
  }, [loadUsers]);

  const handleFreezeConfirm = async () => {
    if (!selectedUser?.wallet_id) return;
    setActionLoading(true);
    try {
      if (modalAction === 'FREEZE') {
        await fetchApi(`/admin/wallets/${selectedUser.wallet_id}/freeze`, {
          method: 'PUT',
          body: JSON.stringify({ reason }),
        });
      } else if (modalAction === 'UNFREEZE') {
        await fetchApi(`/admin/wallets/${selectedUser.wallet_id}/unfreeze`, {
          method: 'PUT',
        });
      }
      setModalAction(null);
      setSelectedUser(null);
      loadUsers();
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
    return new Date(isoStr).toLocaleDateString('vi-VN');
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      {/* Page Header */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <h1 style={{ fontSize: '24px', fontWeight: 800, color: 'var(--text-primary)', letterSpacing: '-0.5px' }}>
            Quản Lý Tài Khoản & Ví (ADM-02)
          </h1>
          <p style={{ fontSize: '13px', color: 'var(--text-secondary)', marginTop: '4px' }}>
            Tra cứu thông tin, giám sát số dư và can thiệp khóa/mở ví khẩn cấp
          </p>
        </div>

        {/* Search Bar */}
        <div style={{ position: 'relative', width: '320px' }}>
          <input
            type="text"
            className="form-input"
            style={{ paddingLeft: '38px' }}
            placeholder="Tìm theo SĐT hoặc họ tên..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
          <svg
            style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }}
            width="16"
            height="16"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
          >
            <circle cx="11" cy="11" r="8"></circle>
            <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
          </svg>
        </div>
      </div>

      {error && (
        <div style={{ padding: '14px 18px', background: 'var(--danger-bg)', border: '1px solid rgba(239, 68, 68, 0.3)', borderRadius: 'var(--radius-md)', color: '#f87171', fontSize: '14px' }}>
          {error}
        </div>
      )}

      {/* Users Table */}
      <div className="glass-panel" style={{ padding: '20px' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px' }}>
          <div style={{ fontSize: '13px', color: 'var(--text-secondary)' }}>
            Tổng số: <strong style={{ color: 'var(--text-primary)' }}>{totalElements}</strong> người dùng
          </div>
        </div>

        <div className="table-wrapper">
          <table className="admin-table">
            <thead>
              <tr>
                <th>Số Điện Thoại</th>
                <th>Họ Và Tên</th>
                <th>Vai Trò</th>
                <th>Trạng Thái User</th>
                <th>Số Dư Ví</th>
                <th>Trạng Thái Ví</th>
                <th>Ngày Tạo</th>
                <th>Hành Động</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={8} style={{ textAlign: 'center', padding: '40px', color: 'var(--text-muted)' }}>
                    Đang tải danh sách người dùng...
                  </td>
                </tr>
              ) : users.length === 0 ? (
                <tr>
                  <td colSpan={8} style={{ textAlign: 'center', padding: '40px', color: 'var(--text-muted)' }}>
                    Không tìm thấy người dùng phù hợp
                  </td>
                </tr>
              ) : (
                users.map((u) => (
                  <tr key={u.user_id}>
                    <td>
                      <Link href={`/users/${u.user_id}`} style={{ color: '#38bdf8', fontWeight: 600 }} className="mono">
                        {u.phone_number}
                      </Link>
                    </td>
                    <td style={{ fontWeight: 600 }}>{u.full_name}</td>
                    <td>
                      <span style={{ fontSize: '12px', fontWeight: 600, color: u.role === 'ADMIN' ? '#818cf8' : 'var(--text-secondary)' }}>
                        {u.role}
                      </span>
                    </td>
                    <td>
                      <Badge status={u.user_status} />
                    </td>
                    <td style={{ fontWeight: 700, color: 'var(--text-primary)' }}>
                      {formatVnd(u.balance)}
                    </td>
                    <td>
                      <Badge status={u.wallet_status} />
                    </td>
                    <td style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>
                      {formatDate(u.created_at)}
                    </td>
                    <td>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                        <Link href={`/users/${u.user_id}`} className="btn btn-secondary" style={{ padding: '6px 10px', fontSize: '12px' }}>
                          Chi tiết
                        </Link>
                        {u.wallet_status === 'ACTIVE' && (
                          <button
                            onClick={() => {
                              setSelectedUser(u);
                              setModalAction('FREEZE');
                            }}
                            className="btn btn-danger"
                            style={{ padding: '6px 10px', fontSize: '12px' }}
                          >
                            Khóa ví
                          </button>
                        )}
                        {u.wallet_status === 'FROZEN' && (
                          <button
                            onClick={() => {
                              setSelectedUser(u);
                              setModalAction('UNFREEZE');
                            }}
                            className="btn btn-success"
                            style={{ padding: '6px 10px', fontSize: '12px' }}
                          >
                            Mở khóa
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination Controls */}
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

      {/* Confirmation Modal (FLOW-08) */}
      <Modal
        isOpen={modalAction !== null}
        onClose={() => setModalAction(null)}
        title={modalAction === 'FREEZE' ? 'Xác nhận Đóng Băng Ví' : 'Xác nhận Mở Khóa Ví'}
      >
        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          <p style={{ fontSize: '14px', color: 'var(--text-secondary)', lineHeight: '1.5' }}>
            {modalAction === 'FREEZE' ? (
              <>
                Bạn có chắc chắn muốn đóng băng ví của người dùng{' '}
                <strong style={{ color: 'var(--text-primary)' }}>{selectedUser?.full_name}</strong> (
                <span className="mono">{selectedUser?.phone_number}</span>)? Khi bị khóa, người dùng sẽ không thể chuyển tiền hay thanh toán VietQR.
              </>
            ) : (
              <>
                Bạn có chắc chắn muốn mở khóa lại ví cho người dùng{' '}
                <strong style={{ color: 'var(--text-primary)' }}>{selectedUser?.full_name}</strong> (
                <span className="mono">{selectedUser?.phone_number}</span>)?
              </>
            )}
          </p>

          {modalAction === 'FREEZE' && (
            <div className="form-group">
              <label className="form-label">Lý do đóng băng ví:</label>
              <textarea
                className="form-input"
                rows={3}
                value={reason}
                onChange={(e) => setReason(e.target.value)}
                placeholder="Nhập lý do nghiệp vụ..."
              />
            </div>
          )}

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '12px' }}>
            <button onClick={() => setModalAction(null)} className="btn btn-secondary" disabled={actionLoading}>
              Hủy bỏ
            </button>
            <button
              onClick={handleFreezeConfirm}
              className={modalAction === 'FREEZE' ? 'btn btn-danger' : 'btn btn-success'}
              disabled={actionLoading}
            >
              {actionLoading ? 'Đang thực hiện...' : modalAction === 'FREEZE' ? 'Xác nhận Khóa Ví' : 'Xác nhận Mở Khóa'}
            </button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
