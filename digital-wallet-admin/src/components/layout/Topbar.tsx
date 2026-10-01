'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { getAdminUser, clearAdminSession } from '@/lib/auth';

export default function Topbar() {
  const router = useRouter();
  const [adminUser, setAdminUser] = useState<{ fullName: string; phoneNumber: string; role: string } | null>(null);
  const [timeStr, setTimeStr] = useState<string>('');

  useEffect(() => {
    const user = getAdminUser();
    if (user) {
      setAdminUser(user);
    }

    const updateClock = () => {
      const now = new Date();
      setTimeStr(now.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit', second: '2-digit' }));
    };
    updateClock();
    const interval = setInterval(updateClock, 1000);
    return () => clearInterval(interval);
  }, []);

  const handleLogout = () => {
    if (confirm('Bạn có chắc chắn muốn đăng xuất khỏi cổng Quản trị?')) {
      clearAdminSession();
      router.push('/login');
    }
  };

  return (
    <header className="admin-topbar">
      {/* Left: System Status & Time */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
        <div style={{
          display: 'flex',
          alignItems: 'center',
          gap: '8px',
          padding: '6px 12px',
          background: 'rgba(255, 255, 255, 0.04)',
          borderRadius: '999px',
          fontSize: '12px',
          color: 'var(--text-secondary)',
          border: '1px solid var(--border-color)',
        }}>
          <span style={{ width: '8px', height: '8px', borderRadius: '50%', background: '#10b981', display: 'inline-block', boxShadow: '0 0 8px #10b981' }}></span>
          <span className="mono">{timeStr} (GMT+7)</span>
        </div>
      </div>

      {/* Right: Admin Profile & Logout */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          <div style={{
            width: '36px',
            height: '36px',
            borderRadius: '50%',
            background: 'linear-gradient(135deg, #4f46e5, #06b6d4)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontWeight: 700,
            fontSize: '14px',
            color: '#fff',
            border: '2px solid rgba(255, 255, 255, 0.1)'
          }}>
            {adminUser?.fullName ? adminUser.fullName.charAt(0) : 'A'}
          </div>
          <div style={{ textAlign: 'left' }}>
            <div style={{ fontSize: '13px', fontWeight: 600, color: 'var(--text-primary)' }}>
              {adminUser?.fullName || 'Quản Trị Viên'}
            </div>
            <div style={{ fontSize: '11px', color: '#06b6d4', fontWeight: 500 }}>
              {adminUser?.role || 'ADMIN'} • {adminUser?.phoneNumber || '0999999999'}
            </div>
          </div>
        </div>

        <button
          onClick={handleLogout}
          className="btn btn-secondary"
          style={{ padding: '8px 14px', fontSize: '13px' }}
          title="Đăng xuất"
        >
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path>
            <polyline points="16 17 21 12 16 7"></polyline>
            <line x1="21" y1="12" x2="9" y2="12"></line>
          </svg>
          Đăng xuất
        </button>
      </div>
    </header>
  );
}
