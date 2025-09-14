# 빌드 스테이지
FROM openjdk:21-jdk-slim AS builder

WORKDIR /app

# Gradle 래퍼와 빌드 파일 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# 소스 코드 복사
COPY src src

# 빌드 실행
RUN chmod +x ./gradlew && ./gradlew clean bootJar -x test

# 실행 스테이지
FROM eclipse-temurin:21-jre-alpine

# curl 설치 (healthcheck용)
RUN apk add --no-cache curl

WORKDIR /app

# 빌드 스테이지에서 JAR 파일 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 포트 노출
EXPOSE 8888

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.jar"]