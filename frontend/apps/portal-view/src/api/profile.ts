import { apiDelete, apiGet, apiPut, http, type ResponseData } from './http';
import type { PageResult } from './dictionary';
import type { ForumPost } from './forum';

/** Khớp ProfileResponse của backend — không có email, tuổi hay gói dịch vụ: ai cũng xem được trang này */
export interface Profile {
  userId: string;
  fullName: string;
  bio?: string;
  /** Ảnh tĩnh; với ảnh đại diện video thì là khung hình đầu */
  avatarUrl?: string;
  /** Video ≤ 5 giây, phát lặp không tiếng */
  avatarVideoUrl?: string;
  accountKind?: 'CHILD' | 'ADULT' | 'PARENT' | 'TEACHER';
  /** Chỉ có khi chuyên môn VSL đã được xác minh */
  verifiedVslRole?: 'LEARNER' | 'DEAF_NATIVE' | 'TEACHER' | 'INTERPRETER';
  joinedAt: string;
  isMe: boolean;
  stats: {
    posts: number;
    comments: number;
    reactionsReceived: number;
    streakDays: number;
    longestStreakDays: number;
    stars: number;
    level: number;
  };
}

export const myProfileApi = () => apiGet<Profile>('/profiles/me');

export const profileApi = (userId: string) => apiGet<Profile>(`/profiles/${userId}`);

export const profilePostsApi = (userId: string, page = 0, size = 10) =>
  apiGet<PageResult<ForumPost>>(`/profiles/${userId}/posts`, { page, size });

export const updateProfileApi = (params: { fullName: string; bio?: string }) => apiPut<Profile>('/profiles/me', params);

/**
 * Đổi ảnh đại diện. Video phải kèm `poster` (khung hình đầu) và `durationMs`: máy chủ Java
 * không giải mã được video, mọi chỗ hiện ảnh tĩnh (diễn đàn, CMS) đều dùng tấm poster đó.
 */
export const uploadAvatarApi = (file: Blob, opts: { poster?: Blob; durationMs?: number } = {}) => {
  const form = new FormData();
  const duoi = file.type.includes('mp4') ? 'mp4' : file.type.includes('webm') ? 'webm' : file.type.split('/')[1] || 'bin';
  form.append('file', file, `avatar.${duoi}`);
  if (opts.poster) form.append('poster', opts.poster, 'poster.jpg');
  return http
    .post<ResponseData<Profile>>('/profiles/me/avatar', form, {
      params: { durationMs: opts.durationMs },
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 2 * 60 * 1000,
    })
    .then((r) => r.data.data);
};

export const removeAvatarApi = () => apiDelete<Profile>('/profiles/me/avatar');
