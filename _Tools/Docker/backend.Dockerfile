# Stage 1: Build stage using JDK 17
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

# Copy the entire project structure
COPY . .

# Grant execution permissions to gradlew (needed for Linux-based Docker)
RUN chmod +x gradlew

# Build the backend application (skipping tests as they are excluded)
RUN ./gradlew :backend:installDist --no-daemon

# Stage 2: Runtime stage using a slim JRE
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy the built application from the build stage
COPY --from=build /app/backend/build/install/backend ./

# Expose the Ktor port
EXPOSE 8080

# Run the application using the generated shell script
ENTRYPOINT ["bin/backend"]
