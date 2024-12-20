FROM maven:3.8.4-openjdk-17-slim AS build
WORKDIR /app

COPY pom.xml .

RUN mvn dependency:go-offline

COPY src ./src

RUN mvn clean package -DskipTests

FROM openjdk:17-slim
WORKDIR /app

RUN apt-get update && \
    apt-get install -y ffmpeg && \
    apt-get clean

COPY --from=build /app/target/dayquestbackend-0.0.1-SNAPSHOT.jar app.jar

EXPOSE ${SERVER_PORT}

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
