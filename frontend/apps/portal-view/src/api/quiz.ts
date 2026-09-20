import { apiGet, apiPost } from './http';
import type { PageResult } from './dictionary';

export type QuizType = 'LESSON_QUIZ' | 'TOPIC_TEST' | 'PRACTICE';
export type QuestionType = 'VIDEO_TO_WORD' | 'WORD_TO_VIDEO' | 'MULTIPLE_CHOICE' | 'MATCHING' | 'AI_PERFORM';
export type QuizAttemptStatus = 'IN_PROGRESS' | 'SUBMITTED';

export interface QuizSummary {
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
  questionCount: number;
}

export interface QuizBlueprintSummary {
  id: string;
  code: string;
  titleVi: string;
  descriptionVi?: string;
  topicNames: string[];
  questionCount: number;
  passScore: number;
  timeLimitSeconds?: number;
  matchingSignCount: number;
}

export interface QuizOption {
  signId?: string;
  label: string;
  videoUrl?: string;
}

export interface QuizQuestion {
  id: string;
  questionType: QuestionType;
  displayOrder: number;
  points: number;
  signId?: string;
  signWordVi?: string;
  signGloss?: string;
  signVideoUrl?: string;
  signThumbnailUrl?: string;
  promptVi?: string;
  options: QuizOption[];
  /** Chỉ có sau khi nộp bài */
  correctOptionIndex?: number;
}

export interface AchievementBadge {
  id: string;
  code: string;
  nameVi: string;
  iconName?: string;
  descriptionVi?: string;
}

export interface QuizAttempt {
  id: string;
  quizId?: string;
  quizTitleVi?: string;
  blueprintId?: string;
  blueprintTitleVi?: string;
  region: string;
  status: QuizAttemptStatus;
  score?: number;
  maxScore?: number;
  passed?: boolean;
  passScoreRequired: number;
  startedAt: string;
  submittedAt?: string;
  durationSeconds?: number;
  questions: QuizQuestion[];
  /** Song song với questions - null = bỏ trống. Chỉ có sau khi nộp */
  selectedOptionIndexes?: (number | null)[];
  newAchievements: AchievementBadge[];
}

export const quizzesApi = (topicId?: string, page = 0, size = 20) =>
  apiGet<PageResult<QuizSummary>>('/quizzes', { topicId, page, size });

export const quizBlueprintsApi = (page = 0, size = 20) =>
  apiGet<PageResult<QuizBlueprintSummary>>('/quiz-blueprints', { page, size });

export const startAttemptFromQuizApi = (quizId: string, region = 'COMMON') =>
  apiPost<QuizAttempt>(`/learn/quiz-attempts/from-quiz/${quizId}?region=${region}`);

export const startAttemptFromBlueprintApi = (blueprintId: string, region = 'COMMON') =>
  apiPost<QuizAttempt>(`/learn/quiz-attempts/from-blueprint/${blueprintId}?region=${region}`);

export const getAttemptApi = (attemptId: string) =>
  apiGet<QuizAttempt>(`/learn/quiz-attempts/${attemptId}`);

export interface AnswerSubmission {
  questionIndex: number;
  selectedOptionIndex: number | null;
}

export const submitAttemptApi = (attemptId: string, answers: AnswerSubmission[]) =>
  apiPost<QuizAttempt>(`/learn/quiz-attempts/${attemptId}/submit`, { answers });
