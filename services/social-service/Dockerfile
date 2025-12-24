# syntax=docker/dockerfile:1
FROM openjdk:17-jdk-slim AS build
RUN apt-get update && apt-get install -y maven

WORKDIR /app

COPY pom.xml .
COPY services/common/pom.xml services/common/pom.xml
COPY services/social-service/pom.xml services/social-service/pom.xml

RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline -pl services/social-service -am

COPY services/common services/common
COPY services/social-service services/social-service

RUN --mount=type=cache,target=/root/.m2 mvn clean package -pl services/social-service -am -DskipTests

FROM openjdk:17-jdk-slim

WORKDIR /app

COPY --from=build /app/services/social-service/target/*.jar app.jar

EXPOSE 8085

ENTRYPOINT ["java", "-jar", "app.jar"]
