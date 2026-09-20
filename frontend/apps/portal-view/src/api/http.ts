import axios, { type AxiosInstance } from 'axios';

/**
 * Bọc thẳng theo envelope ResponseData<T> của backend:
 * { status, lang, messageCode, data, error, message, timestamp, path }
 */
export interface ResponseData<T> {
  status: number;
  lang?: string;
  messageCode?: string;
  data: T;
  error?: string;
  message?: string;
  timestamp?: string;
  path?: string;
}

const TOKEN_KEY = 'signai_portal_token';

export function getToken(): string | null {
  try {
    return localStorage.getItem(TOKEN_KEY);
  } catch {
    return null;
  }
}

export function setToken(token: string | null) {
  try {
    if (token) localStorage.setItem(TOKEN_KEY, token);
    else localStorage.removeItem(TOKEN_KEY);
  } catch {
    // Trình duyệt chặn localStorage (chế độ ẩn danh nghiêm ngặt) — bỏ qua,
    // người dùng chỉ mất trạng thái đăng nhập giữa các lần tải trang
  }
}

export const http: AxiosInstance = axios.create({
  baseURL: '/api/v1',
  timeout: 15000,
});

http.interceptors.request.use((config) => {
  const token = getToken();
  if (token) {
    config.headers = config.headers ?? {};
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

/**
 * Trả thẳng `data` của envelope, ném lỗi có `.message` tiếng Việt sẵn từ
 * backend — component chỉ việc hiện `error.message`, không tự chế câu báo lỗi.
 */
http.interceptors.response.use(
  (response) => response,
  (error) => {
    const envelope = error?.response?.data as ResponseData<unknown> | undefined;
    const message = envelope?.message || envelope?.error || 'Có lỗi xảy ra, thử lại sau.';
    if (error.response?.status === 401) {
      setToken(null);
    }
    return Promise.reject(new Error(message));
  },
);

export async function apiGet<T>(url: string, params?: Record<string, unknown>): Promise<T> {
  const res = await http.get<ResponseData<T>>(url, { params });
  return res.data.data;
}

export async function apiPost<T>(url: string, data?: unknown): Promise<T> {
  const res = await http.post<ResponseData<T>>(url, data);
  return res.data.data;
}

export async function apiPut<T>(url: string, data?: unknown): Promise<T> {
  const res = await http.put<ResponseData<T>>(url, data);
  return res.data.data;
}

export async function apiDelete<T>(url: string): Promise<T> {
  const res = await http.delete<ResponseData<T>>(url);
  return res.data.data;
}
