'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';

interface NavItem {
  label: string;
  href: string;
  icon: (active: boolean) => React.ReactNode;
}

export default function Sidebar() {
  const pathname = usePathname();

  const navItems: NavItem[] = [
    {
      label: 'Tổng Quan',
      href: '/dashboard',
      icon: (active) => (
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke={active ? '#06b6d4' : 'currentColor'} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <rect x="3" y="3" width="7" height="7"></rect>
          <rect x="14" y="3" width="7" height="7"></rect>
          <rect x="14" y="14" width="7" height="7"></rect>
          <rect x="3" y="14" width="7" height="7"></rect>
        </svg>
      ),
    },
    {
      label: 'Người Dùng & Ví',
      href: '/users',
      icon: (active) => (
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke={active ? '#06b6d4' : 'currentColor'} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
          <circle cx="9" cy="7" r="4"></circle>
          <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
          <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
        </svg>
      ),
    },
    {
      label: 'Lịch Sử Giao Dịch',
      href: '/transactions',
      icon: (active) => (
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke={active ? '#06b6d4' : 'currentColor'} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <polyline points="22 12 18 12 15 21 9 3 6 12 2 12"></polyline>
        </svg>
      ),
    },
    {
      label: 'Đối Soát Sổ Cái',
      href: '/reconciliation',
      icon: (active) => (
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke={active ? '#06b6d4' : 'currentColor'} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
          <polyline points="14 2 14 8 20 8"></polyline>
          <line x1="16" y1="13" x2="8" y2="13"></line>
          <line x1="16" y1="17" x2="8" y2="17"></line>
          <polyline points="10 9 9 9 8 9"></polyline>
        </svg>
      ),
    },
    {
      label: 'Cấu Hình Hệ Thống',
      href: '/settings',
      icon: (active) => (
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke={active ? '#06b6d4' : 'currentColor'} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <circle cx="12" cy="12" r="3"></circle>
          <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"></path>
        </svg>
      ),
    },
  ];

  return (
    <aside className="admin-sidebar">
      {/* Brand Header */}
      <div style={{ padding: '24px 20px', borderBottom: '1px solid var(--border-color)', display: 'flex', alignItems: 'center', gap: '12px' }}>
        <div style={{
          width: '40px',
          height: '40px',
          borderRadius: '12px',
          background: 'var(--primary-gradient)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          boxShadow: '0 4px 16px rgba(79, 70, 229, 0.4)'
        }}>
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#fff" strokeWidth="2.5">
            <rect x="2" y="4" width="20" height="16" rx="2"></rect>
            <line x1="2" y1="10" x2="22" y2="10"></line>
          </svg>
        </div>
        <div>
          <div style={{ fontWeight: 800, fontSize: '16px', letterSpacing: '-0.3px', background: 'linear-gradient(90deg, #fff, #94a3b8)', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent' }}>
            MiniWallet Hub
          </div>
          <div style={{ fontSize: '11px', color: '#06b6d4', fontWeight: 600, letterSpacing: '0.5px', textTransform: 'uppercase' }}>
            Admin Portal
          </div>
        </div>
      </div>

      {/* Nav List */}
      <nav style={{ padding: '20px 12px', display: 'flex', flexDirection: 'column', gap: '6px', flex: 1 }}>
        {navItems.map((item) => {
          const active = pathname === item.href || pathname.startsWith(item.href + '/');
          return (
            <Link
              key={item.href}
              href={item.href}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '12px',
                padding: '12px 16px',
                borderRadius: 'var(--radius-md)',
                fontSize: '14px',
                fontWeight: active ? 600 : 500,
                color: active ? '#ffffff' : 'var(--text-secondary)',
                background: active ? 'linear-gradient(90deg, rgba(79, 70, 229, 0.25) 0%, rgba(6, 182, 212, 0.1) 100%)' : 'transparent',
                border: active ? '1px solid rgba(79, 70, 229, 0.4)' : '1px solid transparent',
                transition: 'all 0.2s ease',
              }}
            >
              {item.icon(active)}
              <span>{item.label}</span>
            </Link>
          );
        })}
      </nav>

      {/* Bottom Footer Info */}
      <div style={{ padding: '16px 20px', borderTop: '1px solid var(--border-color)', fontSize: '11px', color: 'var(--text-muted)' }}>
        <div>ACID Ledger • 2-Tier Lock</div>
        <div style={{ color: '#10b981', display: 'flex', alignItems: 'center', gap: '6px', marginTop: '4px' }}>
          <span style={{ width: '6px', height: '6px', borderRadius: '50%', background: '#10b981', display: 'inline-block' }}></span>
          Hệ thống hoạt động bình thường
        </div>
      </div>
    </aside>
  );
}
