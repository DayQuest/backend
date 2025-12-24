# syntax=docker/dockerfile:1
FROM openjdk:17-jdk-slim AS build
RUN apt-get update && apt-get install -y maven

WORKDIR /app

# Copy parent pom and all modules
COPY pom.xml .
COPY services/common/pom.xml services/common/pom.xml
COPY services/discovery-server/pom.xml services/discovery-server/pom.xml

# Download dependencies
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline -pl services/discovery-server -am

# Copy source code
COPY services/common services/common
COPY services/discovery-server services/discovery-server

# Build
RUN --mount=type=cache,target=/root/.m2 mvn clean package -pl services/discovery-server -am -DskipTests

FROM openjdk:17-jdk-slim

WORKDIR /app

COPY --from=build /app/services/discovery-server/target/*.jar app.jar

EXPOSE 8761

ENTRYPOINT ["java", "-jar", "app.jar"]
