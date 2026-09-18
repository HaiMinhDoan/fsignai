/**
 * Kiểu dữ liệu cho module Xác thực.
 * Khớp 1-1 với DTO backend (dto/request/auth, dto/response/auth).
 */

export type AgeRange =
  | 'UNDER_12'
  | 'AGE_12_17'
  | 'AGE_18_24'
  | 'AGE_25_34'
  | 'AGE_35_49'
  | 'AGE_50_PLUS';

/** Quan hệ với cộng đồng khiếm thính — câu hỏi bắt buộc lúc đăng ký */
export type UserType = 'DEAF_HOH' | 'FAMILY' | 'FRIEND' | 'CAREGIVER' | 'OTHER';

export type Region = 'NORTH' | 'CENTRAL' | 'SOUTH' | 'COMMON';

/** Khai báo chuyên môn VSL — chỉ ảnh hưởng trọng số góp ý, KHÔNG cấp quyền quản trị */
export type VslRole = 'LEARNER' | 'DEAF_NATIVE' | 'TEACHER' | 'INTERPRETER';

export type VslRoleStatus = 'SELF_DECLARED' | 'PENDING' | 'VERIFIED' | 'REJECTED';

export type UserStatus = 'ACTIVE' | 'PENDING_VERIFICATION' | 'DISABLED' | 'BANNED';

export interface RegisterParams {
  email: string;
  password: string;
  fullName: string;
  ageRange?: AgeRange;
  userType: UserType;
  region: Region;
  vslRole: VslRole;
  /** Nơi công tác / số chứng chỉ, để admin xác minh vai trò chuyên môn */
  vslRoleEvidence?: string;
}

export interface LoginParams {
  email: string;
  password: string;
}

export interface RoleInfo {
  roleName: string;
  value: string;
}

export interface UserProfile {
  // Phần khớp GetUserInfoModel của vben
  userId: string;
  username: string;
  realName: string;
  avatar?: string;
  desc?: string;
  homePath?: string;
  roles: RoleInfo[];

  // Phần riêng của SignAI
  email: string;
  emailVerified: boolean;
  userType: UserType;
  ageRange?: AgeRange;
  region: Region;
  address?: string;
  vslRole: VslRole;
  vslRoleStatus: VslRoleStatus;
  status: UserStatus;
  /** Đã trả lời 4 câu onboarding chưa — dùng để điều hướng sau đăng nhập */
  onboardingCompleted: boolean;
  lastLoginAt?: string;
  createdAt?: string;
}

export interface AuthTokenResult {
  /** Tên là `token` (không phải accessToken) để khớp LoginResultModel của vben */
  token: string;
  refreshToken: string;
  tokenType: string;
  /** Thời gian sống của access token, tính bằng giây */
  expiresIn: number;
  user: UserProfile;
}

export interface ForgotPasswordParams {
  email: string;
}

export interface ResetPasswordParams {
  token: string;
  newPassword: string;
}

export interface ChangePasswordParams {
  currentPassword: string;
  newPassword: string;
}
