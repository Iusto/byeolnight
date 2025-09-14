FROM eclipse-temurin:21-jre-alpine

# curl 설치 (healthcheck용)
RUN apk add --no-cache curl

WORKDIR /app

# 로컬에서 빌드된 JAR 파일 복사
COPY build/libs/*.jar app.jar

# 포트 노출
EXPOSE 8888

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.jar"]