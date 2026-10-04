import { apiGet, apiPost } from './http';
import type { PageResult } from './dictionary';

export interface Plan {
  code: 'FREE' | 'PREMIUM';
  nameVi: string;
  taglineVi: string;
  descriptionVi: string;
  /** VND; 0 với gói Free */
  price: number;
  durationDays?: number;
  benefits: string[];
  /** Gói Premium gồm toàn bộ quyền lợi của gói Free */
  includesFree: boolean;
}

export interface Subscription {
  plan: 'FREE' | 'PREMIUM';
  premium: boolean;
  premiumUntil?: string;
  staff: boolean;
}

export interface PaymentOrder {
  id: string;
  planCode: string;
  amount: number;
  /** Nội dung chuyển khoản — phải ghi ĐÚNG chuỗi này để hệ thống tự nhận tiền */
  paymentCode: string;
  status: 'PENDING' | 'PAID' | 'EXPIRED';
  qrUrl: string;
  bankCode: string;
  accountNumber: string;
  accountName?: string;
  expiresAt: string;
  paidAt?: string;
  premiumUntil?: string;
}

export const plansApi = () => apiGet<Plan[]>('/plans');

export const mySubscriptionApi = () => apiGet<Subscription>('/payments/me');

export const createOrderApi = (planCode: 'PREMIUM') => apiPost<PaymentOrder>('/payments/orders', { planCode });

export const orderStatusApi = (id: string) => apiGet<PaymentOrder>(`/payments/orders/${id}`);

/** Một dòng lịch sử giao dịch. Đơn chờ đã quá hạn được máy chủ trả về EXPIRED */
export interface PaymentHistoryItem {
  id: string;
  planCode: string;
  amount: number;
  paidAmount?: number;
  paymentCode: string;
  status: 'PENDING' | 'PAID' | 'EXPIRED' | 'CANCELLED';
  durationDays?: number;
  createdAt: string;
  expiresAt: string;
  paidAt?: string;
}

export const myPaymentHistoryApi = (page = 0, size = 10, status?: string) =>
  apiGet<PageResult<PaymentHistoryItem>>('/payments/me/orders', { page, size, status });
