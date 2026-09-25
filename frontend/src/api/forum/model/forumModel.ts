export interface ForumCategoryModel {
  id: string;
  slug: string;
  nameVi: string;
  descriptionVi?: string;
  iconName?: string;
  displayOrder: number;
  isLocked: boolean;
  isPublished: boolean;
  postCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface ForumCategorySaveParams {
  slug?: string;
  nameVi: string;
  descriptionVi?: string;
  iconName?: string;
  displayOrder?: number;
  isLocked?: boolean;
  isPublished?: boolean;
}

export type ForumPostStatus = 'DRAFT' | 'PENDING_REVIEW' | 'PUBLISHED' | 'HIDDEN' | 'REMOVED';

/** Video ký hiệu hoặc ảnh người dùng gắn vào bài — kiểm duyệt viên phải xem được */
export interface ForumMediaModel {
  id: string;
  kind: 'VIDEO' | 'IMAGE';
  source: 'WEBCAM_RECORDED' | 'FILE_UPLOAD';
  url: string;
  thumbnailUrl?: string;
  durationMs?: number;
}

export interface ForumPostModel {
  id: string;
  categoryId: string;
  categoryNameVi: string;
  authorId: string;
  authorName: string;
  /** Rỗng khi tác giả ra hiệu tiêu đề bằng video thay vì gõ chữ */
  titleVi?: string;
  bodyMd?: string;
  titleMedia?: ForumMediaModel;
  media?: ForumMediaModel[];
  viewCount: number;
  commentCount: number;
  reactionCount: number;
  isPinned: boolean;
  isLocked: boolean;
  status: ForumPostStatus;
  publishedAt?: string;
  lastActivityAt: string;
  createdAt: string;
}

export type ForumReportStatus = 'OPEN' | 'REVIEWING' | 'RESOLVED' | 'DISMISSED';
export type ForumReportTargetType = 'POST' | 'COMMENT' | 'MEDIA' | 'USER';
export type ForumReportReason = 'SPAM' | 'ABUSE' | 'WRONG_SIGN' | 'OFF_TOPIC' | 'SENSITIVE' | 'OTHER';

export interface ForumReportModel {
  id: string;
  reporterId: string;
  reporterName: string;
  targetType: ForumReportTargetType;
  targetId: string;
  targetPreview?: string;
  targetStillExists: boolean;
  reason: ForumReportReason;
  note?: string;
  status: ForumReportStatus;
  handledById?: string;
  handledByName?: string;
  handledAt?: string;
  handlerNote?: string;
  createdAt: string;
}
