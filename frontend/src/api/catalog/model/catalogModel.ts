/**
 * Kiểu dữ liệu cho module Khoá học & Bài học.
 * Khớp 1-1 với DTO backend (dto/request/catalog, dto/response/catalog).
 */
import type { SignLevel, SortCriteria } from '@/api/content/model/contentModel';

export type LessonItemType = 'SIGN' | 'VIDEO' | 'TEXT' | 'PRACTICE' | 'QUIZ';

export interface CourseModel {
  id: string;
  slug: string;
  titleVi: string;
  descriptionVi?: string;

  coverFileId?: string;
  coverUrl?: string;

  topicId?: string;
  topicNameVi?: string;

  level: SignLevel;
  displayOrder: number;
  /** true = do hệ thống sinh tự động từ chủ đề, không phải người soạn tay */
  generated: boolean;
  isPublished: boolean;

  lessonCount?: number;
  /** Chỉ có ở API chi tiết */
  lessons?: LessonModel[];

  createdAt?: string;
  updatedAt?: string;
}

export interface CourseSaveParams {
  slug?: string;
  titleVi: string;
  descriptionVi?: string;
  coverFileId?: string;
  topicId?: string;
  level?: SignLevel;
  displayOrder?: number;
  isPublished?: boolean;
}

export interface CourseFilterParams {
  page?: number;
  size?: number;
  sorts?: SortCriteria[];
  filters?: Array<{
    fieldName: string;
    operation: string;
    value?: unknown;
    logicType?: string;
  }>;
}

export interface LessonModel {
  id: string;
  courseId: string;
  courseTitleVi?: string;
  titleVi: string;
  descriptionVi?: string;
  displayOrder: number;
  estimatedMinutes: number;
  generated: boolean;
  isPublished: boolean;
  itemCount?: number;
  /** Chỉ có ở API chi tiết bài học */
  items?: LessonItemModel[];
  createdAt?: string;
  updatedAt?: string;
}

export interface LessonSaveParams {
  titleVi: string;
  descriptionVi?: string;
  displayOrder?: number;
  estimatedMinutes?: number;
  isPublished?: boolean;
}

export interface LessonItemModel {
  id: string;
  lessonId: string;
  itemType: LessonItemType;
  displayOrder: number;

  signId?: string;
  signWordVi?: string;
  signGloss?: string;
  /** Backend kèm sẵn để danh sách không phải gọi thêm API cho từng dòng */
  signPrimaryVideoUrl?: string;
  /** Ảnh đại diện của video — dùng làm poster cho thẻ <video> */
  signThumbnailUrl?: string;

  quizId?: string;
  quizTitleVi?: string;

  contentJson?: Record<string, unknown>;
}

/** Sắp xếp bằng cách gửi toàn bộ danh sách id theo đúng thứ tự mới */
export interface ReorderParams {
  orderedIds: string[];
}

export interface GenerateCoursesParams {
  topicIds?: string[];
  signsPerLesson?: number;
  onlyPublishedSigns?: boolean;
  dryRun?: boolean;
}

export interface GenerateTopicPlan {
  topicNameVi: string;
  signCount: number;
  lessonCount: number;
  /** Có giá trị nghĩa là chủ đề bị bỏ qua, và đây là lý do */
  skippedReason?: string;
}

export interface GenerateCoursesResult {
  dryRun: boolean;
  coursesCreated: number;
  coursesUpdated: number;
  lessonsCreated: number;
  itemsCreated: number;
  topicsSkipped: number;
  plans: GenerateTopicPlan[];
}

export interface AssignTopicsParams {
  signIds: string[];
  topicIds: string[];
  /** true = thay thế toàn bộ chủ đề cũ; mặc định là thêm vào */
  replace?: boolean;
  /** Chỉ có tác dụng khi chọn đúng một chủ đề */
  setPrimary?: boolean;
}

// ==================== Gói từ (bản đồ đảo) ====================

export interface WordPackItemModel {
  id: string;
  displayOrder: number;
  signId: string;
  signWordVi: string;
  signGloss?: string;
  signPrimaryVideoUrl?: string;
  signThumbnailUrl?: string;
}

export interface WordPackModel {
  id: string;
  code: string;
  titleVi: string;
  descriptionVi?: string;

  coverFileId?: string;
  coverUrl?: string;

  topicId?: string;
  topicNameVi?: string;

  level: SignLevel;
  islandColor?: string;
  iconName?: string;
  displayOrder: number;

  unlockAfterPackId?: string;
  unlockAfterPackTitleVi?: string;

  passScore: number;
  isPublished: boolean;

  itemCount?: number;
  /** Chỉ có ở API chi tiết */
  items?: WordPackItemModel[];

  createdAt?: string;
  updatedAt?: string;
}

export interface WordPackSaveParams {
  code?: string;
  titleVi: string;
  descriptionVi?: string;
  coverFileId?: string;
  topicId?: string;
  level?: SignLevel;
  islandColor?: string;
  iconName?: string;
  displayOrder?: number;
  unlockAfterPackId?: string;
  passScore?: number;
  isPublished?: boolean;
}

export interface WordPackFilterParams {
  page?: number;
  size?: number;
  sorts?: SortCriteria[];
  filters?: Array<{ fieldName: string; operation: string; value?: unknown }>;
}
