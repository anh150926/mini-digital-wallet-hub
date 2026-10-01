import React from 'react';

interface BadgeProps {
  status: string;
}

export default function Badge({ status }: BadgeProps) {
  let badgeClass = 'badge-info';
  let label = status;

  switch (status) {
    case 'ACTIVE':
    case 'SUCCESS':
      badgeClass = 'badge-success';
      label = status === 'ACTIVE' ? 'Hoạt động' : 'Thành công';
      break;
    case 'FROZEN':
    case 'LOCKED':
    case 'FAILED':
      badgeClass = 'badge-danger';
      label = status === 'FROZEN' ? 'Đã khóa' : status === 'LOCKED' ? 'Tạm khóa' : 'Thất bại';
      break;
    case 'PENDING':
    case 'PROCESSING':
    case 'SUSPENDED':
      badgeClass = 'badge-warning';
      label = status === 'PENDING' ? 'Chờ xử lý' : status === 'PROCESSING' ? 'Đang xử lý' : 'Tạm dừng';
      break;
    default:
      badgeClass = 'badge-info';
  }

  return <span className={`badge ${badgeClass}`}>{label}</span>;
}
