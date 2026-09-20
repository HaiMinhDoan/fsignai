# syntax=docker/dockerfile:1.7

# Một Dockerfile, hai sản phẩm: CMS (vben admin) và Portal (app học cho bé).
# Cả hai nằm chung một pnpm workspace nên dùng chung một lần cài phụ thuộc.
#   docker build --target cms    .
#   docker build --target portal .

# ---------- Cài phụ thuộc cho cả workspace ----------
FROM node:20-alpine AS deps
RUN corepack enable
WORKDIR /app
COPY . .
# cache mount cho kho pnpm: không phải layer nên vẫn dùng lại được sau `build --no-cache`.
# --frozen-lockfile: bản build phải khớp đúng pnpm-lock.yaml, không tự nâng phiên bản.
RUN --mount=type=cache,target=/root/.local/share/pnpm/store,sharing=locked \
    pnpm install --frozen-lockfile

# ---------- Build CMS ----------
FROM deps AS build-cms
# --mode deploy đọc .env.deploy: tắt mock, giữ VITE_GLOB_API_URL=/basic-api (nginx chuyển tiếp)
RUN NODE_OPTIONS=--max-old-space-size=4096 pnpm vite build --mode deploy

# ---------- Build Portal ----------
FROM deps AS build-portal
WORKDIR /app/apps/portal-view
# Tải WASM + model MediaPipe vào public/mediapipe (~20MB): portal tự phục vụ, KHÔNG gọi CDN Google
RUN node scripts/setup-mediapipe.mjs
RUN NODE_OPTIONS=--max-old-space-size=4096 npx vite build

# ---------- Ảnh chạy: CMS ----------
FROM nginx:alpine AS cms
COPY docker/nginx-cms.conf /etc/nginx/conf.d/default.conf
COPY --from=build-cms /app/dist /usr/share/nginx/html
EXPOSE 5173
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD wget -q --spider http://127.0.0.1:5173/ || exit 1

# ---------- Ảnh chạy: Portal ----------
FROM nginx:alpine AS portal
COPY docker/nginx-portal.conf /etc/nginx/conf.d/default.conf
COPY --from=build-portal /app/apps/portal-view/dist /usr/share/nginx/html
EXPOSE 5174
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD wget -q --spider http://127.0.0.1:5174/ || exit 1
