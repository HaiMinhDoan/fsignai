import { defHttp } from '@/utils/http/axios';
import type { ErrorMessageMode } from '#/axios';
import type {
  AuthTokenResult,
  ChangePasswordParams,
  ForgotPasswordParams,
  LoginParams,
  RegisterParams,
  ResetPasswordParams,
  UserProfile,
} from './model/authModel';

enum Api {
  REGISTER = '/api/v1/auth/register',
  LOGIN = '/api/v1/auth/login',
  REFRESH = '/api/v1/auth/refresh',
  LOGOUT = '/api/v1/auth/logout',
  ME = '/api/v1/auth/me',
  FORGOT_PASSWORD = '/api/v1/auth/forgot-password',
  RESET_PASSWORD = '/api/v1/auth/reset-password',
  CHANGE_PASSWORD = '/api/v1/auth/change-password',
}

export function registerApi(params: RegisterParams, mode: ErrorMessageMode = 'modal') {
  return defHttp.post<AuthTokenResult>(
    { url: Api.REGISTER, data: params },
    { errorMessageMode: mode },
  );
}

export function loginApi(params: LoginParams, mode: ErrorMessageMode = 'modal') {
  return defHttp.post<AuthTokenResult>({ url: Api.LOGIN, data: params }, { errorMessageMode: mode });
}

/**
 * Làm mới access token.
 * Backend XOAY refresh token: mỗi lần gọi sẽ trả về refresh token mới và
 * thu hồi cái cũ ngay, nên phải lưu lại giá trị mới.
 */
export function refreshTokenApi(refreshToken: string) {
  return defHttp.post<AuthTokenResult>(
    { url: Api.REFRESH, data: { refreshToken } },
    { errorMessageMode: 'none' },
  );
}

export function logoutApi(refreshToken?: string) {
  return defHttp.post<void>(
    { url: Api.LOGOUT, data: { refreshToken: refreshToken ?? '' } },
    { errorMessageMode: 'none' },
  );
}

export function getUserProfileApi() {
  return defHttp.get<UserProfile>({ url: Api.ME }, { errorMessageMode: 'none' });
}

export function forgotPasswordApi(params: ForgotPasswordParams) {
  return defHttp.post<void>({ url: Api.FORGOT_PASSWORD, data: params });
}

export function resetPasswordApi(params: ResetPasswordParams) {
  return defHttp.post<void>({ url: Api.RESET_PASSWORD, data: params });
}

export function changePasswordApi(params: ChangePasswordParams) {
  return defHttp.post<void>(
    { url: Api.CHANGE_PASSWORD, data: params },
    { successMessageMode: 'message' },
  );
}
