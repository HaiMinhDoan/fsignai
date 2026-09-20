import { apiGet, apiPost } from './http';

/** 33 điểm × [x, y, z, visibility]; null khi khung này MediaPipe không thấy người */
export type PoseLandmarks = number[][];
/** 21 điểm × [x, y, z] */
export type HandLandmarks = number[][];

export interface LandmarkFrame {
  pose: PoseLandmarks | null;
  hands: HandLandmarks[];
}

/** Toạ độ x, y chuẩn hoá [0,1] theo khung hình CHƯA lật gương; aspect = rộng / cao */
export interface LandmarkClip {
  aspect: number;
  frames: LandmarkFrame[];
}

export interface AiFeedback {
  handshape: number;
  location: number;
  movement: number;
  /** Lời khuyên tiếng Việt, đã dựng ở server */
  hints: string[];
  hintCodes: string[];
}

export interface AiVerifyResult {
  resultId: string;
  signId: string;
  score: number;
  passed: boolean;
  feedback: AiFeedback;
  modelVersion: string;
  checkedAt: string;
}

export interface AiReadiness {
  ready: boolean;
  exemplarCount: number;
}

export const aiReadinessApi = (signId: string) =>
  apiGet<AiReadiness>(`/ai-check/signs/${signId}/readiness`);

export const aiVerifyApi = (signId: string, landmarks: LandmarkClip) =>
  apiPost<AiVerifyResult>('/ai-check/verify', { signId, landmarks });

export const aiFeedbackApi = (resultId: string, verdict: 'AGREE' | 'DISAGREE') =>
  apiPost<void>(`/ai-check/results/${resultId}/feedback`, { verdict });
