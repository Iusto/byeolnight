FROM openjdk:21-jdk-slim

# curl 설치 (healthcheck용)
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Gradle 빌드 결과물 복사
COPY build/libs/*.jar app.jar

# 포트 노출
EXPOSE 8888

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.jar"]