import { getAdminToken, clearAdminSession } from './auth';
import { ApiResponse } from '@/types/admin';

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api/v1';

export class ApiError extends Error {
  code: string;
  status: number;
  details?: unknown;

  constructor(code: string, message: string, status: number, details?: unknown) {
    super(message);
    this.name = 'ApiError';
    this.code = code;
    this.status = status;
    this.details = details;
  }
}

export async function fetchApi<T>(
  endpoint: string,
  options: RequestInit = {}
): Promise<T> {
  const token = getAdminToken();

  const headers: HeadersInit = {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...options.headers,
  };

  const response = await fetch(`${API_BASE_URL}${endpoint}`, {
    ...options,
    headers,
  });

  if (response.status === 401) {
    clearAdminSession();
    if (typeof window !== 'undefined' && !window.location.pathname.includes('/login')) {
      window.location.href = '/login';
    }
  }

  const json: ApiResponse<T> = await response.json().catch(() => ({
    success: false,
    data: null as unknown as T,
    error: { code: 'NETWORK_ERROR', message: 'Lỗi giao tiếp máy chủ' },
    timestamp: new Date().toISOString(),
  }));

  if (!response.ok || !json.success) {
    const error = json.error || { code: 'HTTP_' + response.status, message: response.statusText };
    throw new ApiError(error.code, error.message, response.status, error.details);
  }

  return json.data;
}
