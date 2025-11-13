# Stage 1: Build the JAR
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app

# Copy everything
COPY . .

# Build the gateway JAR, skip tests and Spotless
RUN mvn clean package -DskipTests -Dspotless.check.skip=true

# Stage 2: Run the app
FROM openjdk:21-slim
WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /app/target/api-gateway-0.0.1-SNAPSHOT.jar app.jar

# Expose the port set by Render
EXPOSE ${PORT:-8080}

ENTRYPOINT ["java", "-jar", "app.jar"]




