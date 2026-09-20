export type BlogCategory = 'BLOG' | 'MISSION' | 'GUIDE' | 'NEWS';

export interface BlogPostModel {
  id: string;
  slug: string;
  titleVi: string;
  excerptVi?: string;
  contentMd: string;
  coverUrl?: string;
  authorName?: string;
  category: BlogCategory;
  viewCount: number;
  isPublished: boolean;
  publishedAt?: string;
  createdAt: string;
}

export interface BlogPostSaveParams {
  slug?: string;
  titleVi: string;
  excerptVi?: string;
  contentMd: string;
  coverFileId?: string;
  category: BlogCategory;
  isPublished: boolean;
}
