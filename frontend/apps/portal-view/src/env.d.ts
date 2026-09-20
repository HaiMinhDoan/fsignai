/// <reference types="vite/client" />

/**
 * Khai báo để TypeScript hiểu `import anh from '@/assets/.../x.png'`.
 * Vite biến mỗi ảnh thành một URL chuỗi lúc build; không có dòng này thì
 * vue-tsc báo "Cannot find module" dù chạy vẫn đúng.
 */
declare module '*.vue' {
  import type { DefineComponent } from 'vue';
  const component: DefineComponent<Record<string, unknown>, Record<string, unknown>, unknown>;
  export default component;
}
