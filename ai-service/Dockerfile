FROM python:3.12-slim

WORKDIR /app

# MediaPipe cần libGL và libglib dù đã dùng opencv-python-headless; curl để HEALTHCHECK
RUN apt-get update \
 && apt-get install -y --no-install-recommends libgl1 libglib2.0-0 curl \
 && rm -rf /var/lib/apt/lists/*

COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt

COPY app ./app
COPY scripts ./scripts
COPY tools ./tools

# Tải 2 model MediaPipe (~13MB) ngay lúc build: chạy thật không phải chờ tải, và
# không phụ thuộc mạng ra ngoài khi container khởi động.
RUN python scripts/fetch_models.py

RUN useradd --system --create-home --uid 10002 aisvc && chown -R aisvc:aisvc /app
USER aisvc

ENV PYTHONUNBUFFERED=1 \
    TZ=Asia/Ho_Chi_Minh \
    AI_MAX_CONCURRENCY=4

EXPOSE 8001

HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
  CMD curl -fsS http://localhost:8001/health || exit 1

# 1 worker + nhiều luồng: MediaPipe đã tự dùng nhiều lõi, thêm worker chỉ nhân đôi bộ nhớ model.
# Muốn chạy song song nhiều hơn thì tăng AI_MAX_CONCURRENCY, đừng tăng số worker.
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8001"]
