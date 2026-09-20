import { fileURLToPath, URL } from 'node:url';
import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

// Web học tập cho người dùng cuối — app Vite RIÊNG với apps/admin (thư mục
// src/ ở gốc). Không dùng chung layout Ant Design Vue của admin: người học
// VSL cần giao diện nhiều hình ảnh/video lớn, ít chữ, tương phản cao — ép vào
// layout sidebar/table của admin sẽ phải gỡ bỏ gần hết những gì template cho
// sẵn. Xem docs/00-project-plan.md §3.2.
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5174,
    proxy: {
      // Backend Spring Boot đã có sẵn tiền tố /api/v1/... nên không cần rewrite
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
});
