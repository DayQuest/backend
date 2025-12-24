# syntax=docker/dockerfile:1
FROM openjdk:17-jdk-slim AS build
RUN apt-get update && apt-get install -y maven

WORKDIR /app

COPY pom.xml .
COPY services/common/pom.xml services/common/pom.xml
COPY services/user-service/pom.xml services/user-service/pom.xml

RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline -pl services/user-service -am

COPY services/common services/common
COPY services/user-service services/user-service

RUN --mount=type=cache,target=/root/.m2 mvn clean package -pl services/user-service -am -DskipTests

FROM openjdk:17-jdk-slim

WORKDIR /app

COPY --from=build /app/services/user-service/target/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
