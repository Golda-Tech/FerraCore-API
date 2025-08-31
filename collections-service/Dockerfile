# Stage 1: Build the JAR
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app

# Copy everything
COPY . .

# Skip Spotless when building in Docker
RUN mvn clean package -DskipTests -Dspotless.check.skip=true

# Stage 2: Run the app
FROM openjdk:21-slim
WORKDIR /app
COPY --from=builder /app/target/collections-service-0.0.1-SNAPSHOT.jar app.jar
# Render uses PORT environment variable
EXPOSE ${PORT:-8082}
ENTRYPOINT ["java", "-jar", "app.jar"]
