FROM eclipse-temurin:21-jre-alpine

RUN apk add --no-cache tzdata curl
ENV TZ=Asia/Seoul

WORKDIR /app
COPY build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
