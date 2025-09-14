# 빌드 스테이지
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY src src

# Gradle wrapper에 실행 권한 부여
RUN chmod +x ./gradlew

# 애플리케이션 빌드
RUN ./gradlew clean bootJar -x test

# 실행 스테이지
FROM eclipse-temurin:21-jre-alpine

# 시간대 설정
RUN apk add --no-cache tzdata
ENV TZ=Asia/Seoul

WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
