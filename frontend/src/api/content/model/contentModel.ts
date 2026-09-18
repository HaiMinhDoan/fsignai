/**
 * Kiểu dữ liệu cho module Quản lý nội dung.
 * Khớp 1-1 với DTO phía backend Spring Boot để đổi tên trường ở một nơi
 * là biết ngay chỗ nào hỏng, thay vì để lệch âm thầm.
 */

// ==================== Enum ====================
// Giữ đúng tên hằng của backend (constant/enums/*.java) — không dịch sang
// tiếng Việt ở đây, phần hiển thị nằm riêng trong *.data.ts.

export type SignLevel = 'BEGINNER' | 'BASIC' | 'INTERMEDIATE' | 'ADVANCED';

export type Region = 'NORTH' | 'CENTRAL' | 'SOUTH' | 'COMMON';

export type UnitType = 'LETTER' | 'NUMBER' | 'WORD' | 'PHRASE' | 'SENTENCE';

export type WordType =
  | 'DANH_TU'
  | 'DONG_TU'
  | 'TINH_TU'
  | 'SO_TU'
  | 'DAI_TU'
  | 'PHO_TU'
  | 'QUAN_HE_TU'
  | 'LUONG_TU'
  | 'CHI_TU'
  | 'TRO_TU'
  | 'TINH_THAI_TU'
  | 'THAN_TU'
  | 'KHONG_XAC_DINH';

export type WordSubtype = 'CHUNG' | 'RIENG' | 'DON_VI';

export type SignDomain =
  | 'MATH'
  | 'GEOGRAPHY'
  | 'COUNTRY'
  | 'MEDICAL'
  | 'LEGAL'
  | 'SCHOOL'
  | 'IT'
  | 'SPORT'
  | 'RELIGION';

export type SignSource = 'MOET_QIPEDC' | 'SELF_RECORDED' | 'IMPORTED' | 'MANUAL';

export type ReviewStatus = 'UNREVIEWED' | 'APPROVED' | 'NEEDS_FIX';

export type ViewAngle = 'FRONT' | 'LEFT' | 'RIGHT';

export type IngestStatus = 'PENDING' | 'DOWNLOADING' | 'READY' | 'FAILED';

export type TopicCategory = 'SIMPLE_SIGN' | 'COMPLEX_SIGN' | 'SITUATION';

export type DuplicateStrategy = 'SKIP' | 'OVERWRITE' | 'CREATE_DRAFT';

export type SortDirection = 'ASC' | 'DESC';

// ==================== Chung ====================

/** Khớp PageResponse<T> của backend — cũng chính là shape BasicFetchResult của vben */
export interface PageResult<T> {
  items: T[];
  total: number;
  page: number;
  size: number;
  totalPages: number;
}

export interface SortCriteria {
  fieldName: string;
  direction: SortDirection;
}

// ==================== Chủ đề ====================

export interface TopicModel {
  id: string;
  parentId?: string;
  parentNameVi?: string;
  slug: string;
  nameVi: string;
  descriptionVi?: string;
  iconName?: string;
  iconUrl?: string;
  coverUrl?: string;
  category: TopicCategory;
  displayOrder: number;
  isPublished: boolean;
  signCount?: number;
  children?: TopicModel[];
  createdAt?: string;
  updatedAt?: string;
}

export interface TopicSaveParams {
  parentId?: string;
  /** Bỏ trống thì backend tự sinh từ nameVi: "Giao tiếp hằng ngày" -> "giao-tiep-hang-ngay" */
  slug?: string;
  nameVi: string;
  descriptionVi?: string;
  iconName?: string;
  iconFileId?: string;
  coverFileId?: string;
  category: TopicCategory;
  displayOrder: number;
  isPublished: boolean;
}

// ==================== Video ký hiệu ====================

export interface SignVideoModel {
  id: string;
  signId: string;
  region: Region;
  viewAngle: ViewAngle;
  videoUrl?: string;
  thumbnailUrl?: string;
  signerLabel?: string;
  durationMs?: number;
  width?: number;
  height?: number;
  captionVi?: string;
  isPrimary: boolean;
  ingestStatus: IngestStatus;
  sourceUrl?: string;
  ingestError?: string;
  createdAt?: string;
}

// ==================== Từ vựng ====================

export interface TopicRef {
  id: string;
  slug: string;
  nameVi: string;
}

export interface SignRelationModel {
  id: string;
  relatedSignId: string;
  relatedGloss: string;
  relatedWordVi: string;
  relationType: 'RELATED' | 'SYNONYM' | 'ANTONYM' | 'EASILY_CONFUSED';
}

export interface SignModel {
  id: string;
  gloss: string;
  wordVi: string;
  wordEn?: string;
  descriptionVi?: string;
  noteVi?: string;

  level: SignLevel;
  unitType: UnitType;
  wordType: WordType;
  wordSubtype?: WordSubtype;
  domain?: SignDomain;
  handCount: number;

  primaryTopicId?: string;
  primaryTopicNameVi?: string;
  topics?: TopicRef[];

  source: SignSource;
  sourceRef?: string;

  isPublished: boolean;
  reviewStatus: ReviewStatus;
  reviewedAt?: string;

  thumbnailUrl?: string;
  /** Ba chấm B/T/N trên bảng: vùng miền nào đã có video */
  availableRegions?: Region[];
  /** Đã có exemplar READY chưa — cột "AI" trên bảng */
  aiReady?: boolean;

  videos?: SignVideoModel[];
  relations?: SignRelationModel[];

  createdAt?: string;
  updatedAt?: string;
}

export interface SignSaveParams {
  /** Bỏ trống thì backend tự sinh từ wordVi: "gia đình" -> "GIA_DINH" */
  gloss?: string;
  wordVi: string;
  wordEn?: string;
  descriptionVi?: string;
  noteVi?: string;
  level: SignLevel;
  unitType: UnitType;
  wordType: WordType;
  wordSubtype?: WordSubtype;
  domain?: SignDomain;
  primaryTopicId?: string;
  /** null = giữ nguyên, mảng rỗng = xoá hết chủ đề */
  topicIds?: string[];
  handCount: number;
  source: SignSource;
  sourceRef?: string;
  isPublished: boolean;
}

export interface SignSearchParams {
  /** Backend tự bỏ dấu, nên gõ "dia chi" vẫn ra "địa chỉ" */
  keyword?: string;
  topicId?: string;
  level?: SignLevel;
  unitType?: UnitType;
  wordType?: WordType;
  domain?: SignDomain;
  source?: SignSource;
  reviewStatus?: ReviewStatus;
  isPublished?: boolean;
  /** Lọc từ CHƯA có video ở vùng miền này */
  missingVideoRegion?: Region;
  /** Lọc từ CHƯA chấm điểm AI được */
  missingExemplar?: boolean;
  sorts?: SortCriteria[];
  page?: number;
  size?: number;
}

// ==================== Nhập hàng loạt ====================

export interface SignImportRow {
  rowNumber?: number;
  gloss?: string;
  wordVi?: string;
  wordEn?: string;
  unitType?: string;
  wordType?: string;
  wordSubtype?: string;
  domain?: string;
  level?: string;
  descriptionVi?: string;
  /** Danh sách slug ngăn cách bởi dấu phẩy: "gia-dinh,co-ban" */
  topics?: string;
  /** Mã video gốc trên qipedc.moet.gov.vn, ví dụ "W00665B" */
  videoId?: string;
  region?: string;
}

export interface SignImportParams {
  rows: SignImportRow[];
  duplicateStrategy: DuplicateStrategy;
  /** true = chỉ xem trước kết quả, KHÔNG ghi vào CSDL */
  dryRun: boolean;
}

export interface SignImportResult {
  dryRun: boolean;
  totalRows: number;
  createdCount: number;
  updatedCount: number;
  skippedCount: number;
  failedCount: number;
  errors: Array<{
    rowNumber?: number;
    wordVi?: string;
    message: string;
  }>;
}
