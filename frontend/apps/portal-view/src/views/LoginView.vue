<template>
  <div class="auth-page">
    <form class="auth-card" novalidate @submit.prevent="handleSubmit">
      <RouterLink to="/" class="auth-brand">
        <span class="auth-brand-mark"><SiIcon name="hand" :size="26" /></span>
        <span class="auth-brand-text">SignAI</span>
      </RouterLink>

      <MascotWave :size="80" label="đang chào đón">Bé quay lại rồi!</MascotWave>

      <h1>Đăng nhập</h1>
      <p class="auth-subtitle">Học và luyện tập ngôn ngữ ký hiệu</p>

      <label class="auth-field">
        <span>Email</span>
        <input v-model="email" type="email" autocomplete="email" required :aria-invalid="!!error" />
      </label>

      <label class="auth-field">
        <span>Mật khẩu</span>
        <input v-model="password" type="password" autocomplete="current-password" required />
      </label>

      <p v-if="error" class="auth-error" role="alert">
        <SiIcon name="lock" :size="20" />
        <span>{{ error }}</span>
      </p>

      <button class="auth-submit" type="submit" :disabled="loading">
        <SiIcon v-if="!loading" name="sparkles" :size="22" />
        <span>{{ loading ? 'Đang đăng nhập…' : 'Vào học thôi' }}</span>
      </button>

      <p class="auth-switch">
        Chưa có tài khoản?
        <RouterLink to="/dang-ky">Đăng ký ngay</RouterLink>
      </p>
    </form>
  </div>
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  import { useRoute, useRouter } from 'vue-router';
  import { useAuthStore } from '@/stores/auth';
  import MascotWave from '@/components/MascotWave.vue';
  import SiIcon from '@/components/SiIcon.vue';

  defineOptions({ name: 'LoginView' });

  const auth = useAuthStore();
  const router = useRouter();
  const route = useRoute();

  const email = ref('');
  const password = ref('');
  const loading = ref(false);
  const error = ref('');

  async function handleSubmit() {
    error.value = '';
    loading.value = true;
    try {
      await auth.login(email.value, password.value);
      const redirect = (route.query.redirect as string) || '/';
      router.push(redirect);
    } catch (e) {
      error.value = (e as Error).message;
    } finally {
      loading.value = false;
    }
  }
</script>

<style scoped>
  /* Kiểu dáng dùng chung nằm ở src/styles/auth.css — ở đây chỉ chỉnh linh vật */
  .auth-card :deep(.mascot) {
    margin-bottom: 14px;
  }
</style>
