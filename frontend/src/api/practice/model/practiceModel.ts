/**
 * Kiểu dữ liệu cho module Luyện tập (đề và câu hỏi).
 * Khớp 1-1 với DTO backend (dto/request/practice, dto/response/practice).
 */
import type { SignLevel, SortCriteria, UnitType, WordType } from '@/api/content/model/contentModel';

export type QuizType = 'LESSON_QUIZ' | 'TOPIC_TEST' | 'PRACTICE';

export type QuestionType =
  | 'VIDEO_TO_WORD'
  | 'WORD_TO_VIDEO'
  | 'MULTIPLE_CHOICE'
  | 'MATCHING'
  | 'AI_PERFORM';

export type DistractorStrategy = 'SAME_TOPIC' | 'EASILY_CONFUSED' | 'MIXED';

export interface QuizOptionModel {
  signId?: string;
  label: string;
  /** Backend kèm sẵn để màn hình làm bài không phải gọi thêm API cho từng lựa chọn */
  videoUrl?: string;
}

export interface QuizQuestionModel {
  id: string;
  quizId: string;
  questionType: QuestionType;
  displayOrder: number;
  points: number;

  signId: string;
  signWordVi?: string;
  signGloss?: string;
  signVideoUrl?: string;
  /** Ảnh đại diện của video — dùng làm poster cho thẻ <video> */
  signThumbnailUrl?: string;

  promptVi?: string;
  options: QuizOptionModel[];
  /** Chỉ có ở API quản trị — API làm bài của người học không trả trường này */
  correctOptionIndex?: number;
}

export interface QuizModel {
  id: string;
  lessonId?: string;
  lessonTitleVi?: string;
  courseId?: string;
  courseTitleVi?: string;
  topicId?: string;
  topicNameVi?: string;

  titleVi: string;
  descriptionVi?: string;
  quizType: QuizType;
  passScore: number;
  timeLimitSeconds?: number;
  isPublished: boolean;

  questionCount?: number;
  /** Chỉ có ở API chi tiết */
  questions?: QuizQuestionModel[];

  createdAt?: string;
  updatedAt?: string;
}

export interface QuizSaveParams {
  lessonId?: string;
  topicId?: string;
  titleVi: string;
  descriptionVi?: string;
  quizType?: QuizType;
  passScore?: number;
  timeLimitSeconds?: number;
  isPublished?: boolean;
}

export interface QuizQuestionSaveParams {
  questionType: QuestionType;
  signId: string;
  promptVi?: string;
  options?: Array<{ signId?: string; label?: string }>;
  correctOptionIndex?: number;
  points?: number;
  displayOrder?: number;
}

export interface GenerateQuestionsParams {
  topicIds?: string[];
  signIds?: string[];
  questionCount?: number;
  questionTypeMix?: Partial<Record<QuestionType, number>>;
  optionCount?: number;
  distractorStrategy?: DistractorStrategy;
  requireVideo?: boolean;
  dryRun?: boolean;
}

export interface GenerateQuestionsResult {
  dryRun: boolean;
  created: number;
  signsSkipped: number;
  warnings: string[];
  questions: QuizQuestionModel[];
}

export interface QuizFilterParams {
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

// ===== Cấu hình đề trộn (QuizBlueprint) =====
// Khai báo LUẬT sinh đề (chủ đề, cấp độ, tỉ lệ dạng câu...) thay vì soạn từng
// câu — hệ thống rút ngẫu nhiên từ kho theo luật này mỗi khi người học thi.

export interface QuizBlueprintModel {
  id: string;
  code?: string;
  titleVi: string;
  descriptionVi?: string;

  topicIds: string[];
  /** Tên chủ đề tương ứng, để bảng/form khỏi phải tra ngược */
  topicNames: string[];

  levels: SignLevel[];
  unitTypes: UnitType[];
  /** null = không giới hạn từ loại */
  wordTypes: WordType[] | null;

  questionTypeMix: Partial<Record<QuestionType, number>>;
  questionCount: number;
  optionCount: number;
  passScore: number;
  timeLimitSeconds?: number;
  distractorStrategy: DistractorStrategy;
  avoidRecentDays: number;
  isActive: boolean;

  /**
   * Số từ vựng thực sự khớp bộ lọc — tính bằng đúng luật sẽ dùng lúc sinh đề
   * (kể cả điều kiện phải có video). Cấu hình đòi 20 câu mà số này chỉ có 6
   * thì đề sinh ra sẽ thiếu câu — hiện ngay ở đây thay vì để phát hiện lúc thi.
   */
  matchingSignCount: number;

  createdAt?: string;
  updatedAt?: string;
}

export interface QuizBlueprintSaveParams {
  code?: string;
  titleVi: string;
  descriptionVi?: string;
  topicIds?: string[];
  levels?: SignLevel[];
  unitTypes?: UnitType[];
  wordTypes?: WordType[];
  questionTypeMix?: Partial<Record<QuestionType, number>>;
  questionCount?: number;
  optionCount?: number;
  passScore?: number;
  timeLimitSeconds?: number;
  distractorStrategy?: DistractorStrategy;
  avoidRecentDays?: number;
  isActive?: boolean;
}
