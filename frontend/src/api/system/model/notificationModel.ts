export type NotificationType =
  | 'LESSON_REMINDER'
  | 'STREAK_WARNING'
  | 'STREAK_LOST'
  | 'NEW_COURSE'
  | 'ACHIEVEMENT'
  | 'FORUM_REPLY'
  | 'MODERATION_RESULT'
  | 'ROLE_VERIFIED'
  | 'SYSTEM';

export type NotificationChannel = 'IN_APP' | 'EMAIL' | 'PUSH';
export type NotificationStatus = 'PENDING' | 'SENT' | 'FAILED' | 'SKIPPED';

export interface NotificationModel {
  id: string;
  userId?: string;
  userName?: string;
  type: NotificationType;
  channel: NotificationChannel;
  titleVi: string;
  bodyVi?: string;
  actionUrl?: string;
  status: NotificationStatus;
  errorMessage?: string;
  sentAt?: string;
  readAt?: string;
  createdAt: string;
}
