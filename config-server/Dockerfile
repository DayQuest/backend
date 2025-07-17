FROM openjdk:24-jdk-slim AS build
RUN apt-get update && apt-get install -y maven

WORKDIR /app

COPY . .

RUN --mount=type=cache,target=/root/.m2 mvn clean package -DskipTests

FROM openjdk:24-jdk-slim

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE ${CONFIG_SERVER_PORT}

ENTRYPOINT ["java", "-jar", "app.jar"]