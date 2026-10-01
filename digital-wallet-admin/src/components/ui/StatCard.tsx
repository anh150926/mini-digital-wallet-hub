import React from 'react';

interface StatCardProps {
  title: string;
  value: string | number;
  subtitle?: string;
  icon: React.ReactNode;
  trend?: {
    value: string;
    positive: boolean;
  };
  accentColor?: string;
}

export default function StatCard({
  title,
  value,
  subtitle,
  icon,
  trend,
  accentColor = '#4f46e5',
}: StatCardProps) {
  return (
    <div
      className="glass-panel"
      style={{
        padding: '24px',
        position: 'relative',
        overflow: 'hidden',
        display: 'flex',
        flexDirection: 'column',
        gap: '12px',
      }}
    >
      <div
        style={{
          position: 'absolute',
          top: '-20px',
          right: '-20px',
          width: '80px',
          height: '80px',
          borderRadius: '50%',
          background: accentColor,
          opacity: 0.12,
          filter: 'blur(20px)',
          pointerEvents: 'none',
        }}
      />

      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <span style={{ fontSize: '13px', fontWeight: 600, color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
          {title}
        </span>
        <div
          style={{
            width: '38px',
            height: '38px',
            borderRadius: '10px',
            background: 'rgba(255, 255, 255, 0.05)',
            border: '1px solid var(--border-color)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: accentColor,
          }}
        >
          {icon}
        </div>
      </div>

      <div style={{ fontSize: '28px', fontWeight: 800, color: 'var(--text-primary)', letterSpacing: '-0.5px' }}>
        {value}
      </div>

      {(subtitle || trend) && (
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '12px', color: 'var(--text-muted)' }}>
          {trend && (
            <span
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: '2px',
                fontWeight: 600,
                color: trend.positive ? 'var(--success)' : 'var(--danger)',
              }}
            >
              {trend.positive ? '↑' : '↓'} {trend.value}
            </span>
          )}
          {subtitle && <span>{subtitle}</span>}
        </div>
      )}
    </div>
  );
}
