# syntax=docker/dockerfile:1
FROM openjdk:17-jdk-slim AS build
RUN apt-get update && apt-get install -y maven

WORKDIR /app

COPY pom.xml .
COPY services/common/pom.xml services/common/pom.xml
COPY services/video-service/pom.xml services/video-service/pom.xml

RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline -pl services/video-service -am

COPY services/common services/common
COPY services/video-service services/video-service

RUN --mount=type=cache,target=/root/.m2 mvn clean package -pl services/video-service -am -DskipTests

FROM openjdk:17-jdk-slim

RUN apt-get update && apt-get install -y ffmpeg

WORKDIR /app

COPY --from=build /app/services/video-service/target/*.jar app.jar

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]
