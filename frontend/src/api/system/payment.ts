import { defHttp } from '@/utils/http/axios';
import type { PageResult } from '@/api/content/model/contentModel';

export type PaymentStatus = 'PENDING' | 'PAID' | 'EXPIRED' | 'CANCELLED';

/** Một đơn thanh toán. Đơn còn PENDING mà đã quá hạn được máy chủ trả về EXPIRED */
export interface PaymentOrderModel {
  id: string;
  planCode: string;
  amount: number;
  paidAmount?: number;
  paymentCode: string;
  status: PaymentStatus;
  durationDays?: number;
  createdAt: string;
  expiresAt: string;
  paidAt?: string;
  sepayTransactionId?: number;
  userId: string;
  userName: string;
  userEmail: string;
}

/** Giao dịch SePay báo về, kể cả giao dịch không khớp đơn nào */
export interface SepayTransactionModel {
  id: string;
  sepayId: number;
  gateway?: string;
  transactionDate?: string;
  accountNumber?: string;
  code?: string;
  content?: string;
  transferType?: string;
  transferAmount?: number;
  referenceCode?: string;
  matchedOrderId?: string;
  matchedPaymentCode?: string;
  matchedUserName?: string;
  note?: string;
  createdAt: string;
}

export interface PaymentSummaryModel {
  revenue: number;
  paidOrders: number;
  pendingOrders: number;
  expiredOrders: number;
  payingUsers: number;
  unmatchedTransactions: number;
}

export const paymentOrdersApi = (params: {
  userId?: string;
  keyword?: string;
  status?: string;
  from?: string;
  to?: string;
  page?: number;
  size?: number;
}) => defHttp.get<PageResult<PaymentOrderModel>>({ url: '/api/v1/admin/payments/orders', params });

export const sepayTransactionsApi = (params: {
  matched?: boolean;
  keyword?: string;
  from?: string;
  to?: string;
  page?: number;
  size?: number;
}) => defHttp.get<PageResult<SepayTransactionModel>>({ url: '/api/v1/admin/payments/transactions', params });

export const paymentSummaryApi = (params: { from?: string; to?: string }) =>
  defHttp.get<PaymentSummaryModel>({ url: '/api/v1/admin/payments/summary', params });
