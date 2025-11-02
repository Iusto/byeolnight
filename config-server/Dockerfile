FROM eclipse-temurin:21-jre-alpine

RUN apk add --no-cache curl tzdata
ENV TZ=Asia/Seoul

WORKDIR /app
COPY build/libs/*.jar app.jar

EXPOSE 8888

ENTRYPOINT ["java", "-Xmx256m", "-Xms128m", "-XX:+UseG1GC", "-jar", "app.jar"]