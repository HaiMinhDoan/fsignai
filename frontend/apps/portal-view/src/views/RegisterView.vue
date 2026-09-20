<template>
  <div class="auth-page">
    <form class="auth-card" novalidate @submit.prevent="handleSubmit">
      <RouterLink to="/" class="auth-brand">
        <span class="auth-brand-mark"><SiIcon name="hand" :size="26" /></span>
        <span class="auth-brand-text">SignAI</span>
      </RouterLink>

      <MascotWave :size="80" label="đang mời vào học">Mình làm quen nhé!</MascotWave>

      <h1>Đăng ký</h1>
      <p class="auth-subtitle">Tạo tài khoản để bắt đầu hành trình</p>

      <fieldset class="auth-field auth-role-field">
        <legend>Bạn là ai?</legend>
        <div class="auth-role-group">
          <button
            v-for="opt in ROLE_OPTIONS"
            :key="opt.value"
            type="button"
            class="auth-role-btn"
            :class="{ 'is-active': accountKind === opt.value }"
            @click="accountKind = opt.value"
          >
            <SiIcon :name="opt.icon" :size="22" />
            <span>{{ opt.label }}</span>
          </button>
        </div>
      </fieldset>

      <label class="auth-field">
        <span>Họ và tên</span>
        <input v-model="fullName" type="text" autocomplete="name" required />
      </label>

      <label class="auth-field">
        <span>Email</span>
        <input v-model="email" type="email" autocomplete="email" required />
      </label>

      <label class="auth-field">
        <span>Mật khẩu</span>
        <input
          v-model="password"
          type="password"
          autocomplete="new-password"
          required
          minlength="8"
        />
        <small>Ít nhất 8 ký tự</small>
      </label>

      <p v-if="error" class="auth-error" role="alert">
        <SiIcon name="lock" :size="20" />
        <span>{{ error }}</span>
      </p>

      <button class="auth-submit" type="submit" :disabled="loading">
        <SiIcon v-if="!loading" name="sparkles" :size="22" />
        <span>{{ loading ? 'Đang tạo tài khoản…' : 'Bắt đầu hành trình' }}</span>
      </button>

      <p class="auth-switch">
        Đã có tài khoản?
        <RouterLink to="/dang-nhap">Đăng nhập</RouterLink>
      </p>
    </form>
  </div>
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  import { useRouter } from 'vue-router';
  import { useAuthStore } from '@/stores/auth';
  import MascotWave from '@/components/MascotWave.vue';
  import SiIcon from '@/components/SiIcon.vue';
  import type { AccountKind } from '@/api/auth';

  defineOptions({ name: 'RegisterView' });

  const ROLE_OPTIONS: { value: AccountKind; label: string; icon: string }[] = [
    { value: 'CHILD', label: 'Học sinh', icon: 'book' },
    { value: 'PARENT', label: 'Phụ huynh', icon: 'user' },
    { value: 'TEACHER', label: 'Giáo viên', icon: 'trophy' },
  ];

  const auth = useAuthStore();
  const router = useRouter();

  const accountKind = ref<AccountKind | null>(null);
  const fullName = ref('');
  const email = ref('');
  const password = ref('');
  const loading = ref(false);
  const error = ref('');

  async function handleSubmit() {
    error.value = '';
    if (!accountKind.value) {
      error.value = 'Vui lòng chọn bạn là học sinh, phụ huynh hay giáo viên';
      return;
    }
    loading.value = true;
    try {
      await auth.register({
        fullName: fullName.value,
        email: email.value,
        password: password.value,
        accountKind: accountKind.value,
      });
      router.push('/');
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

  .auth-role-field {
    border: none;
    padding: 0;
    margin: 0;
  }

  .auth-role-field legend {
    padding: 0;
    font-size: 14px;
    font-weight: 600;
    color: var(--si-text, #111c2d);
    margin-bottom: 6px;
  }

  .auth-role-group {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 8px;
  }

  .auth-role-btn {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;
    padding: 10px 6px;
    border-radius: var(--si-kid-radius, 16px);
    border: 2px solid var(--si-border, #d8e3fb);
    background: var(--si-surface, #fff);
    color: var(--si-text-muted, #534434);
    font: inherit;
    font-size: 13px;
    cursor: pointer;
    transition: border-color 0.15s, color 0.15s, background 0.15s;
  }

  .auth-role-btn:hover {
    border-color: var(--si-secondary, #5bb8fe);
  }

  .auth-role-btn.is-active {
    border-color: var(--si-primary, #006398);
    background: var(--si-primary-light, #dee8ff);
    color: var(--si-primary, #006398);
    font-weight: 700;
  }
</style>
