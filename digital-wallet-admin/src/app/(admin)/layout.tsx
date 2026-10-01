'use client';

import React, { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { isAdminAuthenticated } from '@/lib/auth';
import Sidebar from '@/components/layout/Sidebar';
import Topbar from '@/components/layout/Topbar';

export default function AdminLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const router = useRouter();
  const [authorized, setAuthorized] = useState<boolean | null>(null);

  useEffect(() => {
    if (!isAdminAuthenticated()) {
      router.replace('/login');
    } else {
      setAuthorized(true);
    }
  }, [router]);

  if (authorized === null) {
    return (
      <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'var(--bg-primary)' }}>
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '14px' }}>
          <div style={{
            width: '40px',
            height: '40px',
            borderRadius: '50%',
            border: '3px solid rgba(99, 102, 241, 0.2)',
            borderTopColor: '#06b6d4',
            animation: 'spin 1s linear infinite'
          }} />
          <span style={{ color: 'var(--text-secondary)', fontSize: '13px' }}>Đang xác thực quyền Quản trị...</span>
        </div>
        <style jsx>{`
          @keyframes spin {
            to { transform: rotate(360deg); }
          }
        `}</style>
      </div>
    );
  }

  return (
    <div className="admin-shell">
      <Sidebar />
      <div className="admin-main">
        <Topbar />
        <main className="admin-content animate-fade-in">{children}</main>
      </div>
    </div>
  );
}
