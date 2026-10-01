'use client';

import React, { useEffect, useState } from 'react';
import { fetchApi } from '@/lib/api';
import { SystemConfigItem } from '@/types/admin';
import Modal from '@/components/ui/Modal';

export default function SettingsPage() {
  const [configs, setConfigs] = useState<SystemConfigItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Edit Modal State
  const [editingConfig, setEditingConfig] = useState<SystemConfigItem | null>(null);
  const [newValue, setNewValue] = useState('');
  const [reason, setReason] = useState('Điều chỉnh chính sách nghiệp vụ');
  const [saving, setSaving] = useState(false);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  const loadConfigs = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await fetchApi<SystemConfigItem[]>('/admin/configs');
      setConfigs(data || []);
    } catch (err: unknown) {
      if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('Không thể tải danh sách cấu hình');
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadConfigs();
  }, []);

  const openEditModal = (cfg: SystemConfigItem) => {
    setEditingConfig(cfg);
    setNewValue(cfg.config_value);
    setReason('Điều chỉnh hạn mức hoặc biểu phí');
    setSuccessMsg(null);
  };

  const handleSaveConfig = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingConfig) return;
    setSaving(true);
    try {
      await fetchApi(`/admin/configs/${editingConfig.config_key}`, {
        method: 'PUT',
        body: JSON.stringify({
          value: newValue,
          reason: reason,
        }),
      });

      setSuccessMsg(`Cập nhật cấu hình ${editingConfig.config_key} thành công!`);
      setEditingConfig(null);
      loadConfigs();
    } catch (err: unknown) {
      alert(err instanceof Error ? err.message : 'Lỗi khi lưu cấu hình');
    } finally {
      setSaving(false);
    }
  };

  const formatDate = (isoStr: string) => {
    if (!isoStr) return '-';
    return new Date(isoStr).toLocaleString('vi-VN');
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      {/* Title */}
      <div>
        <h1 style={{ fontSize: '24px', fontWeight: 800, color: 'var(--text-primary)', letterSpacing: '-0.5px' }}>
          Cấu Hình Hệ Thống & Biểu Phí (ADM-04)
        </h1>
        <p style={{ fontSize: '13px', color: 'var(--text-secondary)', marginTop: '4px' }}>
          Điều chỉnh hạn mức giao dịch, biểu phí rút tiền và các tham số bảo mật vận hành
        </p>
      </div>

      {successMsg && (
        <div style={{ padding: '14px 18px', background: 'var(--success-bg)', border: '1px solid rgba(16, 185, 129, 0.3)', borderRadius: 'var(--radius-md)', color: '#34d399', fontSize: '14px' }}>
          ✓ {successMsg}
        </div>
      )}

      {error && (
        <div style={{ padding: '14px 18px', background: 'var(--danger-bg)', border: '1px solid rgba(239, 68, 68, 0.3)', borderRadius: 'var(--radius-md)', color: '#f87171', fontSize: '14px' }}>
          {error}
        </div>
      )}

      {/* Configs Table */}
      <div className="glass-panel" style={{ padding: '20px' }}>
        <div className="table-wrapper">
          <table className="admin-table">
            <thead>
              <tr>
                <th>Khóa Cấu Hình (Key)</th>
                <th>Giá Trị Hiện Tại</th>
                <th>Mô Tả Nghiệp Vụ</th>
                <th>Cập Nhật Lần Cuối</th>
                <th>Thao Tác</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={5} style={{ textAlign: 'center', padding: '40px', color: 'var(--text-muted)' }}>
                    Đang tải cấu hình hệ thống...
                  </td>
                </tr>
              ) : configs.length === 0 ? (
                <tr>
                  <td colSpan={5} style={{ textAlign: 'center', padding: '40px', color: 'var(--text-muted)' }}>
                    Không có cấu hình nào trong cơ sở dữ liệu
                  </td>
                </tr>
              ) : (
                configs.map((cfg) => (
                  <tr key={cfg.id}>
                    <td>
                      <span className="mono" style={{ fontSize: '13px', fontWeight: 700, color: '#38bdf8' }}>
                        {cfg.config_key}
                      </span>
                    </td>
                    <td>
                      <span
                        className="mono"
                        style={{
                          display: 'inline-block',
                          padding: '4px 10px',
                          borderRadius: '6px',
                          background: 'rgba(255, 255, 255, 0.06)',
                          fontWeight: 700,
                          color: '#fff',
                        }}
                      >
                        {cfg.config_value}
                      </span>
                    </td>
                    <td style={{ fontSize: '13px', color: 'var(--text-secondary)' }}>
                      {cfg.description}
                    </td>
                    <td style={{ fontSize: '12px', color: 'var(--text-muted)' }}>
                      {formatDate(cfg.updated_at)}
                    </td>
                    <td>
                      <button
                        onClick={() => openEditModal(cfg)}
                        className="btn btn-secondary"
                        style={{ padding: '6px 12px', fontSize: '12px' }}
                      >
                        Chỉnh sửa
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Edit Config Modal */}
      <Modal
        isOpen={editingConfig !== null}
        onClose={() => setEditingConfig(null)}
        title={`Chỉnh Sửa: ${editingConfig?.config_key}`}
      >
        <form onSubmit={handleSaveConfig} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          <div>
            <span style={{ fontSize: '13px', color: 'var(--text-secondary)' }}>Mô tả: </span>
            <span style={{ fontSize: '13px', color: 'var(--text-primary)', fontWeight: 600 }}>
              {editingConfig?.description}
            </span>
          </div>

          <div className="form-group">
            <label className="form-label">Giá trị mới:</label>
            <input
              type="text"
              required
              className="form-input mono"
              value={newValue}
              onChange={(e) => setNewValue(e.target.value)}
              placeholder="Nhập giá trị cấu hình..."
            />
          </div>

          <div className="form-group">
            <label className="form-label">Lý do thay đổi cấu hình:</label>
            <textarea
              className="form-input"
              rows={3}
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              placeholder="Nhập lý do lưu audit log..."
            />
          </div>

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '12px' }}>
            <button
              type="button"
              onClick={() => setEditingConfig(null)}
              className="btn btn-secondary"
              disabled={saving}
            >
              Hủy
            </button>
            <button type="submit" className="btn btn-primary" disabled={saving}>
              {saving ? 'Đang lưu...' : 'Lưu Thay Đổi'}
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
