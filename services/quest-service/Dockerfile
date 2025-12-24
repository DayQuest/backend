# syntax=docker/dockerfile:1
FROM openjdk:17-jdk-slim AS build
RUN apt-get update && apt-get install -y maven

WORKDIR /app

COPY pom.xml .
COPY services/common/pom.xml services/common/pom.xml
COPY services/quest-service/pom.xml services/quest-service/pom.xml

RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline -pl services/quest-service -am

COPY services/common services/common
COPY services/quest-service services/quest-service

RUN --mount=type=cache,target=/root/.m2 mvn clean package -pl services/quest-service -am -DskipTests

FROM openjdk:17-jdk-slim

WORKDIR /app

COPY --from=build /app/services/quest-service/target/*.jar app.jar

EXPOSE 8084

ENTRYPOINT ["java", "-jar", "app.jar"]
