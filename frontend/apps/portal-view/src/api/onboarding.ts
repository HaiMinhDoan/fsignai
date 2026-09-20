import { apiGet, apiPut } from './http';

export type LearnReason = 'FAMILY' | 'FRIENDS' | 'PERSONAL' | 'WORK' | 'BASIC_COMM' | 'OTHER';
export type CurrentLevel = 'BEGINNER' | 'BASIC' | 'INTERMEDIATE' | 'ADVANCED' | 'UNSURE';

export interface OnboardingAnswers {
  learnReason?: LearnReason;
  currentLevel?: CurrentLevel;
  dailyMinutes?: number;
  interestedTopics?: string[];
  completed: boolean;
  completedAt?: string;
}

export interface OnboardingSaveParams {
  learnReason?: LearnReason;
  currentLevel?: CurrentLevel;
  dailyMinutes?: number;
  interestedTopics?: string[];
  complete?: boolean;
}

export const getOnboardingApi = () => apiGet<OnboardingAnswers>('/onboarding');

export const saveOnboardingApi = (params: OnboardingSaveParams) =>
  apiPut<OnboardingAnswers>('/onboarding', params);
