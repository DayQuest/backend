FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /app

# Copy parent pom and all modules
COPY pom.xml .
COPY common/pom.xml common/
COPY eureka-server/pom.xml eureka-server/
COPY config-server/pom.xml config-server/
COPY apigateway/pom.xml apigateway/
COPY user-service/pom.xml user-service/
COPY quest-service/pom.xml quest-service/
COPY notification-service/pom.xml notification-service/
COPY video-service/pom.xml video-service/
COPY social-service/pom.xml social-service/
COPY content-service/pom.xml content-service/

# Download dependencies with retry logic
RUN mvn dependency:go-offline -B -pl quest-service -am \
    -Dmaven.wagon.http.retryHandler.count=5 \
    -Dmaven.wagon.httpconnectionManager.ttlSeconds=120 \
    || mvn dependency:go-offline -B -pl quest-service -am

# Copy source code
COPY common/src common/src
COPY quest-service/src quest-service/src

# Build
RUN mvn clean package -Dmaven.test.skip=true -pl quest-service -am

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=build /app/quest-service/target/*.jar app.jar

EXPOSE 8083

# JVM Optimizations for faster startup
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:TieredStopAtLevel=1", \
  "-XX:+UseG1GC", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-Dspring.backgroundpreinitializer.ignore=true", \
  "-jar", "app.jar"]

