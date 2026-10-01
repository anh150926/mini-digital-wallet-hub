export interface UserSummary {
  user_id: string;
  phone_number: string;
  full_name: string;
  role: 'USER' | 'ADMIN' | 'SUPER_ADMIN' | 'OWNER';
  user_status: 'ACTIVE' | 'SUSPENDED' | 'LOCKED';
  wallet_id?: string;
  balance: number;
  wallet_status: 'ACTIVE' | 'FROZEN' | 'CLOSED' | 'NONE';
  created_at: string;
}

export interface TransactionItem {
  id: string;
  idempotency_key: string;
  source_wallet_id?: string;
  dest_wallet_id?: string;
  source_phone?: string;
  source_name?: string;
  dest_phone?: string;
  dest_name?: string;
  amount: number;
  fee: number;
  type: 'P2P_TRANSFER' | 'TOP_UP' | 'WITHDRAW' | 'QR_PAYMENT';
  status: 'PENDING' | 'PROCESSING' | 'SUCCESS' | 'FAILED' | 'REVERSED';
  description?: string;
  created_at: string;
}

export interface UserDetail extends UserSummary {
  recent_transactions: TransactionItem[];
}

export interface DailyStat {
  date: string;
  count: number;
  volume: number;
}

export interface DashboardStats {
  total_users: number;
  total_active_wallets: number;
  today_transaction_count: number;
  today_transaction_volume: number;
  daily_stats: DailyStat[];
}

export interface ReconciliationReport {
  total_debit: number;
  total_credit: number;
  net_balance: number;
  is_balanced: boolean;
  transaction_count: number;
  debit_entry_count: number;
  credit_entry_count: number;
}

export interface SystemConfigItem {
  id: string;
  config_key: string;
  config_value: string;
  description: string;
  updated_at: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  total_elements: number;
  total_pages: number;
}

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  error?: {
    code: string;
    message: string;
    details?: unknown;
  };
  timestamp: string;
}

export interface AdminAuthSession {
  accessToken: string;
  refreshToken?: string;
  user: {
    id: string;
    phoneNumber: string;
    fullName: string;
    role: string;
  };
}
