import { apiGet, apiPost, apiPut } from './http';

export interface Topic {
  id: string;
  parentId?: string;
  slug: string;
  nameVi: string;
  descriptionVi?: string;
  iconUrl?: string;
  coverUrl?: string;
  category: string;
  isPublished: boolean;
  signCount?: number;
  children?: Topic[];
}

export const topicsApi = () => apiGet<Topic[]>('/topics');

export interface Achievement {
  id: string;
  code: string;
  nameVi: string;
  descriptionVi?: string;
  iconName?: string;
  iconUrl?: string;
  displayOrder?: number;
  /** Chỉ API của người học mới trả hai trường này */
  earned?: boolean;
  earnedAt?: string;
}

/** Cần đăng nhập — gọi khi chưa có token sẽ nhận 401 */
export const myAchievementsApi = () => apiGet<Achievement[]>('/learn/achievements');

export interface LearnerStats {
  streakDays: number;
  longestStreakDays: number;
  stars: number;
  weeklyStars: number;
  level: number;
  levelPoints: number;
  levelTarget: number;
  levelPercent: number;
}

/** Cần đăng nhập. Gộp chuỗi ngày + sao + cấp độ vào một lần gọi cho thanh đầu trang. */
export const learnerStatsApi = () => apiGet<LearnerStats>('/learn/stats');

export interface LeaderboardRow {
  rank: number;
  userId: string;
  fullName: string;
  points: number;
  gamesWon: number;
  isMe: boolean;
}

export interface RewardChest {
  id: string;
  code: string;
  nameVi: string;
  descriptionVi?: string;
  iconName?: string;
  requiredPoints: number;
  opened: boolean;
  openedAt?: string;
  unlockable: boolean;
  pointsShort: number;
}

/** Cả hai đều cần đăng nhập */
export const leaderboardApi = (period: 'weekly' | 'total' = 'weekly', limit = 10) =>
  apiGet<LeaderboardRow[]>('/learn/leaderboard', { period, limit });

export const rewardChestsApi = () => apiGet<RewardChest[]>('/learn/reward-chests');

// ==================== Gói từ (bản đồ đảo) ====================

export interface WordPackItem {
  id: string;
  displayOrder: number;
  signId: string;
  signWordVi: string;
  signGloss?: string;
  signPrimaryVideoUrl?: string;
  signThumbnailUrl?: string;
}

export interface WordPack {
  id: string;
  code: string;
  titleVi: string;
  descriptionVi?: string;
  coverUrl?: string;
  topicId?: string;
  topicNameVi?: string;
  level: string;
  islandColor?: string;
  iconName?: string;
  displayOrder: number;
  unlockAfterPackId?: string;
  unlockAfterPackTitleVi?: string;
  passScore: number;
  itemCount?: number;
  /** Chỉ có ở API chi tiết */
  items?: WordPackItem[];

  /** Bốn trường dưới chỉ có khi đã đăng nhập; khách xem chỉ thấy "unlocked" */
  unlocked?: boolean;
  myStatus?: 'IN_PROGRESS' | 'COMPLETED';
  myItemsCompleted?: number;
  myStars?: number;
}

/** Danh sách cho bản đồ đảo. Khách chưa đăng nhập vẫn xem được, mọi gói có
 *  điều kiện mở khoá sẽ hiện đang khoá vì server chưa biết đã học gì. */
export const wordPacksApi = () => apiGet<WordPack[]>('/word-packs');

export const wordPackDetailApi = (id: string) => apiGet<WordPack>(`/word-packs/${id}`);

/** Cần đăng nhập. Gọi khi người học mở một gói — an toàn gọi lại nhiều lần (idempotent) */
export const wordPackStartApi = (id: string) => apiPost<WordPack>(`/word-packs/${id}/start`);

/** Cần đăng nhập. Chỉ tăng, không lùi — học lại từ cũ không làm tụt tiến độ */
export const wordPackProgressApi = (id: string, itemsCompleted: number) =>
  apiPut<WordPack>(`/word-packs/${id}/progress`, { itemsCompleted });
