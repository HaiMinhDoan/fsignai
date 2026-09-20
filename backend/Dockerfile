# syntax=docker/dockerfile:1.7

# ---------- Giai đoạn build: dùng Maven có sẵn trong image, không dùng ./mvnw ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /src

COPY pom.xml ./
COPY src ./src

# cache mount cho kho Maven: KHÔNG phải layer nên nó SỐNG SÓT qua `docker compose build --no-cache`.
# Nhờ vậy deploy lại chỉ tải phần phụ thuộc đổi mới, thay vì tải lại ~100MB mỗi lần.
#
# Cố ý không dùng `dependency:go-offline`: nó giải mọi phụ thuộc của mọi plugin cho mọi giai đoạn,
# mất hơn 10 phút, trong khi `package` chỉ tải đúng thứ cần.
#
# Bỏ test: test của dự án cần CSDL, MinIO và ai-service thật, không chạy được lúc build image.
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    mvn -B -q clean package -DskipTests

# ---------- Giai đoạn chạy ----------
FROM eclipse-temurin:17-jre
WORKDIR /app

# curl để HEALTHCHECK gọi được /actuator/health
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl tzdata \
 && rm -rf /var/lib/apt/lists/*

# Chạy bằng người dùng thường, không phải root
RUN useradd --system --create-home --uid 10001 spring
USER spring

COPY --from=build --chown=spring:spring /src/target/backend-*.jar app.jar

ENV TZ=Asia/Ho_Chi_Minh \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseContainerSupport"

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=5 \
  CMD curl -fsS http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
