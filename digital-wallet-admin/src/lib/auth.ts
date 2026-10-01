'use client';

import { AdminAuthSession } from '@/types/admin';

const TOKEN_KEY = 'wallet_admin_token';
const USER_KEY = 'wallet_admin_user';

export function saveAdminSession(session: AdminAuthSession) {
  if (typeof window === 'undefined') return;
  localStorage.setItem(TOKEN_KEY, session.accessToken);
  localStorage.setItem(USER_KEY, JSON.stringify(session.user));
}

export function getAdminToken(): string | null {
  if (typeof window === 'undefined') return null;
  return localStorage.getItem(TOKEN_KEY);
}

export function getAdminUser(): AdminAuthSession['user'] | null {
  if (typeof window === 'undefined') return null;
  const raw = localStorage.getItem(USER_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw);
  } catch {
    return null;
  }
}

export function clearAdminSession() {
  if (typeof window === 'undefined') return;
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
}

export function isAdminAuthenticated(): boolean {
  const token = getAdminToken();
  const user = getAdminUser();
  if (!token || !user) return false;
  return ['ADMIN', 'SUPER_ADMIN', 'OWNER'].includes(user.role);
}
