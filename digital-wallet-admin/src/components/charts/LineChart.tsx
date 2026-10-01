'use client';

import React, { useState } from 'react';
import { DailyStat } from '@/types/admin';

interface LineChartProps {
  data: DailyStat[];
}

export default function LineChart({ data }: LineChartProps) {
  const [hoverIndex, setHoverIndex] = useState<number | null>(null);

  if (!data || data.length === 0) {
    return (
      <div style={{ height: '240px', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--text-muted)' }}>
        Không có dữ liệu biểu đồ
      </div>
    );
  }

  const maxVolume = Math.max(...data.map((d) => d.volume), 1000000);
  const chartHeight = 180;
  const chartWidth = 650;
  const paddingX = 40;
  const paddingY = 20;

  const points = data.map((d, index) => {
    const x = paddingX + (index * (chartWidth - 2 * paddingX)) / (data.length - 1 || 1);
    const y = chartHeight - paddingY - (d.volume / maxVolume) * (chartHeight - 2 * paddingY);
    return { x, y, ...d };
  });

  const pathD = points.reduce((acc, curr, index) => {
    return index === 0 ? `M ${curr.x} ${curr.y}` : `${acc} L ${curr.x} ${curr.y}`;
  }, '');

  const areaD = `${pathD} L ${points[points.length - 1].x} ${chartHeight - paddingY} L ${points[0].x} ${chartHeight - paddingY} Z`;

  const formatVnd = (val: number) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val);
  };

  return (
    <div style={{ width: '100%', position: 'relative' }}>
      <svg
        viewBox={`0 0 ${chartWidth} ${chartHeight}`}
        style={{ width: '100%', height: '220px', overflow: 'visible' }}
      >
        <defs>
          <linearGradient id="chartGradient" x1="0%" y1="0%" x2="0%" y2="100%">
            <stop offset="0%" stopColor="#4f46e5" stopOpacity="0.4" />
            <stop offset="100%" stopColor="#06b6d4" stopOpacity="0.0" />
          </linearGradient>
          <filter id="glow" x="-20%" y="-20%" width="140%" height="140%">
            <feGaussianBlur stdDeviation="3" result="glow" />
            <feComposite in="SourceGraphic" in2="glow" operator="over" />
          </filter>
        </defs>

        {/* Grid lines */}
        {[0, 0.25, 0.5, 0.75, 1].map((ratio) => {
          const y = chartHeight - paddingY - ratio * (chartHeight - 2 * paddingY);
          return (
            <line
              key={ratio}
              x1={paddingX}
              y1={y}
              x2={chartWidth - paddingX}
              y2={y}
              stroke="rgba(255, 255, 255, 0.05)"
              strokeDasharray="4 4"
            />
          );
        })}

        {/* Area fill */}
        <path d={areaD} fill="url(#chartGradient)" />

        {/* Line stroke */}
        <path
          d={pathD}
          fill="none"
          stroke="#6366f1"
          strokeWidth="3"
          strokeLinecap="round"
          strokeLinejoin="round"
          filter="url(#glow)"
        />

        {/* Data points */}
        {points.map((p, i) => (
          <g key={i}>
            <circle
              cx={p.x}
              cy={p.y}
              r={hoverIndex === i ? 6 : 4}
              fill="#06b6d4"
              stroke="#ffffff"
              strokeWidth="2"
              style={{ cursor: 'pointer', transition: 'all 0.15s ease' }}
              onMouseEnter={() => setHoverIndex(i)}
              onMouseLeave={() => setHoverIndex(null)}
            />
            {/* Date label */}
            <text
              x={p.x}
              y={chartHeight}
              textAnchor="middle"
              fill="var(--text-muted)"
              fontSize="11"
              fontFamily="sans-serif"
            >
              {p.date.slice(5)}
            </text>
          </g>
        ))}
      </svg>

      {/* Floating Tooltip */}
      {hoverIndex !== null && (
        <div
          style={{
            position: 'absolute',
            top: '0px',
            left: `${(points[hoverIndex].x / chartWidth) * 100}%`,
            transform: 'translateX(-50%)',
            background: 'var(--bg-tertiary)',
            border: '1px solid var(--border-focus)',
            borderRadius: '8px',
            padding: '8px 12px',
            boxShadow: 'var(--shadow-md)',
            pointerEvents: 'none',
            zIndex: 10,
            textAlign: 'center',
          }}
        >
          <div style={{ fontSize: '11px', color: 'var(--text-muted)' }}>{points[hoverIndex].date}</div>
          <div style={{ fontSize: '13px', fontWeight: 700, color: '#38bdf8' }}>
            {formatVnd(points[hoverIndex].volume)}
          </div>
          <div style={{ fontSize: '11px', color: 'var(--text-secondary)' }}>
            {points[hoverIndex].count} giao dịch
          </div>
        </div>
      )}
    </div>
  );
}
