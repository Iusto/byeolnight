# 실행 스테이지 (이미 빌드된 JAR 파일 사용)
FROM eclipse-temurin:21-jre-alpine

# 시간대 설정
RUN apk add --no-cache tzdata
ENV TZ=Asia/Seoul

WORKDIR /app
COPY build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
