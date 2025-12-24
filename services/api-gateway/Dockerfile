# syntax=docker/dockerfile:1
FROM openjdk:17-jdk-slim AS build
RUN apt-get update && apt-get install -y maven

WORKDIR /app

COPY pom.xml .
COPY services/common/pom.xml services/common/pom.xml
COPY services/api-gateway/pom.xml services/api-gateway/pom.xml

RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline -pl services/api-gateway -am

COPY services/common services/common
COPY services/api-gateway services/api-gateway

RUN --mount=type=cache,target=/root/.m2 mvn clean package -pl services/api-gateway -am -DskipTests

FROM openjdk:17-jdk-slim

WORKDIR /app

COPY --from=build /app/services/api-gateway/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
