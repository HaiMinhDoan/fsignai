import { apiGet, apiPost } from './http';

export interface RoleInfo {
  roleName: string;
  value: string;
}

/** Khớp UserProfileResponse của backend — bốn trường đầu đặt tên hơi lạ vì dùng chung với vben-admin */
export interface UserProfile {
  userId: string;
  username: string;
  realName: string;
  avatar?: string;
  roles: RoleInfo[];

  email: string;
  emailVerified?: boolean;
  userType?: string;
  /** CHILD | ADULT | PARENT | TEACHER — chọn lúc đăng ký (học sinh/phụ huynh/giáo viên) */
  accountKind?: string;
  ageRange?: string;
  region?: string;
  address?: string;
  vslRole?: string;
  vslRoleStatus?: string;
  status?: string;
  onboardingCompleted?: boolean;
}

export interface AuthTokenResult {
  token: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: UserProfile;
}

export type AccountKind = 'CHILD' | 'PARENT' | 'TEACHER';

export interface RegisterParams {
  email: string;
  password: string;
  fullName: string;
  accountKind: AccountKind;
}

export const loginApi = (email: string, password: string) =>
  apiPost<AuthTokenResult>('/auth/login', { email, password });

export const registerApi = (params: RegisterParams) => apiPost<AuthTokenResult>('/auth/register', params);

export const meApi = () => apiGet<UserProfile>('/auth/me');
