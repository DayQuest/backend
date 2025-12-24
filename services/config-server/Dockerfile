# syntax=docker/dockerfile:1
FROM openjdk:17-jdk-slim AS build
RUN apt-get update && apt-get install -y maven

WORKDIR /app

COPY pom.xml .
COPY services/common/pom.xml services/common/pom.xml
COPY services/config-server/pom.xml services/config-server/pom.xml

RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline -pl services/config-server -am

COPY services/common services/common
COPY services/config-server services/config-server

RUN --mount=type=cache,target=/root/.m2 mvn clean package -pl services/config-server -am -DskipTests

FROM openjdk:17-jdk-slim

WORKDIR /app

COPY --from=build /app/services/config-server/target/*.jar app.jar

EXPOSE 8888

ENTRYPOINT ["java", "-jar", "app.jar"]
