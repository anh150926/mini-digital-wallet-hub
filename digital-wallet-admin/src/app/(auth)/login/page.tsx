'use client';

import React, { useState } from 'react';
import { useRouter } from 'next/navigation';
import { saveAdminSession } from '@/lib/auth';

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api/v1';

export default function LoginPage() {
  const router = useRouter();
  const [phoneNumber, setPhoneNumber] = useState('0999999999');
  const [password, setPassword] = useState('Admin@123');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);

    try {
      const response = await fetch(`${API_BASE_URL}/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          phone_number: phoneNumber,
          password: password,
        }),
      });

      const data = await response.json();

      if (!response.ok || !data.success) {
        throw new Error(data.error?.message || 'Đăng nhập không thành công');
      }

      const authData = data.data;
      const role = authData.user?.role;

      if (!['ADMIN', 'SUPER_ADMIN', 'OWNER'].includes(role)) {
        throw new Error('Tài khoản này không có quyền Quản trị viên (ROLE_ADMIN)');
      }

      saveAdminSession({
        accessToken: authData.access_token,
        refreshToken: authData.refresh_token,
        user: {
          id: authData.user.id,
          phoneNumber: authData.user.phone_number,
          fullName: authData.user.full_name,
          role: authData.user.role,
        },
      });

      router.push('/dashboard');
    } catch (err: unknown) {
      if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('Đã xảy ra lỗi không xác định');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div
      style={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        background: 'radial-gradient(ellipse at 50% 30%, #1e1b4b 0%, #090d16 70%)',
        padding: '20px',
      }}
    >
      <div
        className="glass-panel animate-fade-in"
        style={{
          width: '100%',
          maxWidth: '440px',
          padding: '40px 36px',
          background: 'rgba(16, 23, 38, 0.92)',
          boxShadow: 'var(--shadow-lg), 0 0 40px rgba(79, 70, 229, 0.2)',
        }}
      >
        {/* Brand Icon & Heading */}
        <div style={{ textAlign: 'center', marginBottom: '32px' }}>
          <div
            style={{
              width: '56px',
              height: '56px',
              margin: '0 auto 16px',
              borderRadius: '16px',
              background: 'var(--primary-gradient)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              boxShadow: '0 8px 24px rgba(79, 70, 229, 0.45)',
            }}
          >
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="#fff" strokeWidth="2.5">
              <rect x="2" y="4" width="20" height="16" rx="2"></rect>
              <line x1="2" y1="10" x2="22" y2="10"></line>
            </svg>
          </div>
          <h1 style={{ fontSize: '24px', fontWeight: 800, color: 'var(--text-primary)', letterSpacing: '-0.5px' }}>
            Cổng Quản Trị Hệ Thống
          </h1>
          <p style={{ fontSize: '13px', color: 'var(--text-secondary)', marginTop: '6px' }}>
            Mini Digital Wallet & QR Payment Hub
          </p>
        </div>

        {/* Error Alert */}
        {error && (
          <div
            style={{
              padding: '12px 16px',
              borderRadius: 'var(--radius-md)',
              background: 'var(--danger-bg)',
              border: '1px solid rgba(239, 68, 68, 0.3)',
              color: '#f87171',
              fontSize: '13px',
              marginBottom: '20px',
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
            }}
          >
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <circle cx="12" cy="12" r="10"></circle>
              <line x1="12" y1="8" x2="12" y2="12"></line>
              <line x1="12" y1="16" x2="12.01" y2="16"></line>
            </svg>
            <span>{error}</span>
          </div>
        )}

        {/* Login Form */}
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label className="form-label">Số điện thoại Admin</label>
            <input
              type="text"
              required
              className="form-input mono"
              value={phoneNumber}
              onChange={(e) => setPhoneNumber(e.target.value)}
              placeholder="VD: 0999999999"
            />
          </div>

          <div className="form-group" style={{ marginBottom: '24px' }}>
            <label className="form-label">Mật khẩu</label>
            <input
              type="password"
              required
              className="form-input"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••"
            />
          </div>

          <button
            type="submit"
            disabled={loading}
            className="btn btn-primary"
            style={{ width: '100%', padding: '13px', fontSize: '15px' }}
          >
            {loading ? 'Đang xác thực...' : 'Đăng Nhập Quản Trị'}
          </button>
        </form>

        {/* Quick Hint */}
        <div
          style={{
            marginTop: '28px',
            padding: '12px',
            background: 'rgba(255, 255, 255, 0.03)',
            borderRadius: 'var(--radius-md)',
            border: '1px dashed var(--border-color)',
            fontSize: '12px',
            color: 'var(--text-muted)',
            textAlign: 'center',
          }}
        >
          <div>Tài khoản Admin mặc định hệ thống:</div>
          <div style={{ color: '#38bdf8', marginTop: '4px' }} className="mono">
            SĐT: 0999999999 • Mật khẩu: Admin@123
          </div>
        </div>
      </div>
    </div>
  );
}
