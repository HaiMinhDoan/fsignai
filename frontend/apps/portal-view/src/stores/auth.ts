import { defineStore } from 'pinia';
import { ref } from 'vue';
import { getToken, setToken } from '@/api/http';
import { loginApi, meApi, registerApi, type RegisterParams, type UserProfile } from '@/api/auth';

export const useAuthStore = defineStore('auth', () => {
  const user = ref<UserProfile | null>(null);
  const isReady = ref(false); // đã thử khôi phục phiên đăng nhập từ token cũ chưa

  const isLoggedIn = () => !!user.value;

  async function login(email: string, password: string) {
    const result = await loginApi(email, password);
    setToken(result.token);
    user.value = result.user;
    return result.user;
  }

  async function register(params: RegisterParams) {
    const result = await registerApi(params);
    setToken(result.token);
    user.value = result.user;
    return result.user;
  }

  function logout() {
    setToken(null);
    user.value = null;
  }

  /** Gọi một lần lúc mở app: còn token cũ thì tải lại thông tin người dùng */
  async function restoreSession() {
    if (isReady.value) return;
    const token = getToken();
    if (token) {
      try {
        user.value = await meApi();
      } catch {
        setToken(null);
        user.value = null;
      }
    }
    isReady.value = true;
  }

  return { user, isReady, isLoggedIn, login, register, logout, restoreSession };
});
