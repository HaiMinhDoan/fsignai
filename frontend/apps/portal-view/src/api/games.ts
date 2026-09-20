import { apiPost } from './http';

export type GameCode = 'MATCH_PAIR' | 'SPEED_GUESS' | 'FINGER_DANCE' | 'MEMORY_FLIP';

export interface GameSign {
  signId: string;
  wordVi: string;
  videoUrl?: string;
  thumbnailUrl?: string;
}

export interface GameOption {
  signId: string;
  label: string;
}

export interface GameQuestion {
  signId: string;
  videoUrl?: string;
  thumbnailUrl?: string;
  /** Đáp án đúng là dòng có signId trùng signId của câu */
  options: GameOption[];
}

export interface GameStart {
  sessionId: string;
  gameCode: GameCode;
  maxScore: number;
  totalRounds: number;
  pairs?: GameSign[];
  questions?: GameQuestion[];
}

export interface GameFinish {
  score: number;
  maxScore: number;
  correctCount: number;
  wrongCount: number;
  durationSeconds: number;
  starsEarned: number;
  totalStars: number;
}

export const startGameApi = (code: GameCode) =>
  apiPost<GameStart>(`/learn/games/${code}/start`, {});

export const finishGameApi = (sessionId: string, correctCount: number, wrongCount: number) =>
  apiPost<GameFinish>(`/learn/games/sessions/${sessionId}/finish`, { correctCount, wrongCount });

export const openChestApi = (chestId: string) =>
  apiPost<{ opened: boolean }>(`/learn/reward-chests/${chestId}/open`);
