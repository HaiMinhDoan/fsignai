import { apiGet, apiPost } from './http';

export interface Flashcard {
  signId: string;
  wordVi: string;
  gloss: string;
  descriptionVi?: string;
  videoUrl?: string;
  thumbnailUrl?: string;
  isNew: boolean;
  dueAt?: string;
}

export interface FlashcardReviewResult {
  nextDueAt: string;
  intervalDays: number;
  easeFactor: number;
  repetitions: number;
}

export type FlashcardResult = 'KNOWN' | 'NEEDS_PRACTICE';

export const dueFlashcardsApi = (topicId?: string, limit = 20) =>
  apiGet<Flashcard[]>('/practice/flashcards', { topicId, limit });

export const reviewFlashcardApi = (signId: string, result: FlashcardResult) =>
  apiPost<FlashcardReviewResult>(`/practice/flashcards/${signId}/review`, { result });
