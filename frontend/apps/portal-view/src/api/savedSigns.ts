import { apiGet } from './http';
import type { PageResult } from './dictionary';

export interface SavedSign {
  id: string;
  signId: string;
  wordVi: string;
  gloss: string;
  level: string;
  videoUrl?: string;
  thumbnailUrl?: string;
  note?: string;
  savedAt: string;
}

export const mySavedSignsApi = (page = 0, size = 20) =>
  apiGet<PageResult<SavedSign>>('/me/saved-signs', { page, size });
