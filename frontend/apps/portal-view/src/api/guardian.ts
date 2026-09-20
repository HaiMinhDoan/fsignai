import { apiGet, apiPost } from './http';

export interface ChildSummary {
  childUserId: string;
  fullName: string;
  ageRange?: string;
  relationship: string;
  /** % mục tiêu tuần, tính từ số ngày có học trong 7 ngày */
  weekPercent: number;
}

export interface DayPoint {
  date: string;
  label: string; // T2..CN
  minutes: number;
  goalMet: boolean;
}

export interface ChildReport {
  childUserId: string;
  fullName: string;
  weekMinutes: number;
  prevWeekMinutes: number;
  masteredSigns: number;
  /** null khi service chấm điểm AI chưa dựng — KHÔNG phải 0 */
  aiAccuracyPercent: number | null;
  streakDays: number;
  week: DayPoint[];
}

export const childrenApi = () => apiGet<ChildSummary[]>('/guardian/children');

export const childReportApi = (childId: string) =>
  apiGet<ChildReport>(`/guardian/children/${childId}/report`);

/** Bé bấm để lấy mã, đọc cho bố mẹ nhập. Mã sống 24 giờ. */
export const createInviteApi = () => apiPost<{ inviteCode: string }>('/guardian/invite');

export const claimChildApi = (inviteCode: string, relationship = 'PARENT') =>
  apiPost<{ childUserId: string }>('/guardian/claim', { inviteCode, relationship });
