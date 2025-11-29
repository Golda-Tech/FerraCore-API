FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests -Dspotless.check.skip=true

FROM openjdk:21-slim
WORKDIR /app
COPY --from=builder /app/target/notification-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE ${PORT:-8084}
ENTRYPOINT ["java", "-jar", "app.jar"]
