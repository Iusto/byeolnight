FROM eclipse-temurin:21-jre-alpine

# curl 설치 (healthcheck용)
RUN apk add --no-cache curl tzdata
ENV TZ=Asia/Seoul

WORKDIR /app

# Gradle 빌드 결과물 복사 (context: ./config-server)
COPY build/libs/*.jar app.jar

# 포트 노출
EXPOSE 8888

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.jar"]