import { apiGet, apiPost } from './http';
import type { PageResult } from './dictionary';

export type SignLevel = 'BEGINNER' | 'BASIC' | 'INTERMEDIATE' | 'ADVANCED';
export type ProgressStatus = 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED';
export type LessonItemType = 'SIGN' | 'VIDEO' | 'TEXT' | 'PRACTICE' | 'QUIZ';

export interface CourseSummary {
  id: string;
  slug: string;
  titleVi: string;
  descriptionVi?: string;
  coverUrl?: string;
  topicId?: string;
  topicNameVi?: string;
  level: SignLevel;
  lessonCount: number;
  myStatus?: ProgressStatus;
  myProgressPercent?: number;
}

export interface CourseDetail extends CourseSummary {
  lessons: LessonSummary[];
}

export interface LessonSummary {
  id: string;
  courseId: string;
  courseTitleVi?: string;
  titleVi: string;
  descriptionVi?: string;
  estimatedMinutes: number;
  itemCount: number;
  myStatus?: ProgressStatus;
  myProgressPercent?: number;
  myLastItemId?: string;
}

export interface LessonItem {
  id: string;
  lessonId: string;
  itemType: LessonItemType;
  displayOrder: number;
  signId?: string;
  signWordVi?: string;
  signGloss?: string;
  signPrimaryVideoUrl?: string;
  signThumbnailUrl?: string;
  quizId?: string;
  quizTitleVi?: string;
  contentJson?: Record<string, unknown>;
}

export interface LessonDetail extends LessonSummary {
  items: LessonItem[];
}

export const coursesApi = (topicId?: string, level?: string, page = 0, size = 20) =>
  apiGet<PageResult<CourseSummary>>('/courses', { topicId, level, page, size });

export const courseDetailApi = (id: string) => apiGet<CourseDetail>(`/courses/${id}`);

export const lessonDetailApi = (id: string) => apiGet<LessonDetail>(`/lessons/${id}`);

export const trackLessonProgressApi = (lessonId: string, itemId: string, timeSpentSeconds?: number) =>
  apiPost<LessonDetail>(`/lessons/${lessonId}/progress`, { itemId, timeSpentSeconds });
