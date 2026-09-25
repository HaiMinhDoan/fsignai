import { apiGet, apiPost, apiPut, apiDelete, http } from './http';
import type { PageResult } from './dictionary';

export interface ForumCategory {
  id: string;
  slug: string;
  nameVi: string;
  descriptionVi?: string;
  iconName?: string;
  displayOrder: number;
  isLocked: boolean;
  isPublished: boolean;
  postCount: number;
}

/**
 * Một video ký hiệu hoặc tấm ảnh người dùng gắn vào bài/bình luận.
 *
 * Với nhiều người điếc, tiếng Việt viết là ngôn ngữ thứ hai — bài viết bằng
 * video ký hiệu không phải tính năng phụ mà là cách nói tự nhiên của họ.
 */
export interface ForumMedia {
  id: string;
  kind: 'VIDEO' | 'IMAGE';
  source: 'WEBCAM_RECORDED' | 'FILE_UPLOAD';
  url: string;
  thumbnailUrl?: string;
  durationMs?: number;
  width?: number;
  height?: number;
  sizeBytes?: number;
  mimeType?: string;
}

export type ForumPostStatus = 'DRAFT' | 'PENDING_REVIEW' | 'PUBLISHED' | 'HIDDEN' | 'REMOVED';

export interface ForumPost {
  id: string;
  categoryId: string;
  categoryNameVi: string;
  authorId: string;
  authorName: string;
  authorAvatarUrl?: string;
  titleVi?: string;
  bodyMd?: string;
  /** Video ký hiệu dùng thay cho tiêu đề chữ */
  titleMedia?: ForumMedia;
  media?: ForumMedia[];
  signId?: string;
  signWordVi?: string;
  viewCount: number;
  commentCount: number;
  reactionCount: number;
  myReaction: boolean;
  isPinned: boolean;
  isLocked: boolean;
  status: ForumPostStatus;
  publishedAt?: string;
  lastActivityAt: string;
  createdAt: string;
}

export interface ForumComment {
  id: string;
  postId: string;
  parentId?: string;
  depth: number;
  authorId: string;
  authorName: string;
  authorAvatarUrl?: string;
  bodyText?: string;
  media?: ForumMedia[];
  reactionCount: number;
  myReaction: boolean;
  status: string;
  createdAt: string;
}

export interface ForumPostSaveParams {
  categoryId: string;
  /** Để trống khi tiêu đề là video ký hiệu */
  titleVi?: string;
  bodyMd?: string;
  titleMediaId?: string;
  mediaIds?: string[];
  signId?: string;
}

export type ReportReason = 'SPAM' | 'ABUSE' | 'WRONG_SIGN' | 'OFF_TOPIC' | 'SENSITIVE' | 'OTHER';

export const forumCategoriesApi = () => apiGet<ForumCategory[]>('/forum/categories');

export const forumPostsApi = (categoryId: string | undefined, page = 0, size = 20) =>
  apiGet<PageResult<ForumPost>>('/forum/posts', { categoryId, page, size });

export const forumPostDetailApi = (id: string) => apiGet<ForumPost>(`/forum/posts/${id}`);

export const forumPostCreateApi = (params: ForumPostSaveParams) => apiPost<ForumPost>('/forum/posts', params);

export const forumPostUpdateApi = (id: string, params: ForumPostSaveParams) =>
  apiPut<ForumPost>(`/forum/posts/${id}`, params);

export const forumPostDeleteApi = (id: string) => apiDelete<void>(`/forum/posts/${id}`);

export const forumCommentsApi = (postId: string) => apiGet<ForumComment[]>(`/forum/posts/${postId}/comments`);

export const forumCommentCreateApi = (
  postId: string,
  bodyText: string,
  parentId?: string,
  mediaIds?: string[],
) => apiPost<ForumComment>(`/forum/posts/${postId}/comments`, { bodyText, parentId, mediaIds });

/**
 * Tải một video ký hiệu hoặc ảnh lên TRƯỚC khi đăng bài.
 *
 * `poster` là khung hình do trình duyệt tự cắt ra từ video: máy chủ Java không
 * giải mã được video, thiếu nó thì danh sách bài hiện một ô đen.
 */
export const forumMediaUploadApi = (
  file: File,
  opts: { poster?: Blob; source?: 'WEBCAM_RECORDED' | 'FILE_UPLOAD'; durationMs?: number; width?: number; height?: number } = {},
) => {
  const form = new FormData();
  form.append('file', file);
  if (opts.poster) form.append('poster', opts.poster, 'poster.jpg');
  return http
    .post<{ data: ForumMedia }>('/forum/media', form, {
      params: {
        source: opts.source,
        durationMs: opts.durationMs,
        width: opts.width,
        height: opts.height,
      },
      headers: { 'Content-Type': 'multipart/form-data' },
      // Video 60 giây qua mạng chậm cần lâu hơn 15 giây mặc định
      timeout: 5 * 60 * 1000,
    })
    .then((r) => r.data.data);
};

export const forumMediaDeleteApi = (id: string) => apiDelete<void>(`/forum/media/${id}`);

export const forumCommentDeleteApi = (id: string) => apiDelete<void>(`/forum/comments/${id}`);

export const forumPostLikeApi = (id: string) => apiPost<{ liked: boolean }>(`/forum/posts/${id}/like`);

export const forumCommentLikeApi = (id: string) => apiPost<{ liked: boolean }>(`/forum/comments/${id}/like`);

export const forumReportApi = (params: {
  targetType: 'POST' | 'COMMENT';
  targetId: string;
  reason: ReportReason;
  note?: string;
}) => apiPost<unknown>('/forum/reports', params);
