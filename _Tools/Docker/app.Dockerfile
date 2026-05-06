# --- Stage 1: Build (JDK 21 + Node.js for wasmJs) ---
FROM eclipse-temurin:21 AS builder
WORKDIR /app

RUN apt-get update && apt-get install -y libatomic1 && rm -rf /var/lib/apt/lists/*

COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts ./
RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon || true

COPY . .
RUN ./gradlew :backend:installDist :owner-web:wasmJsBrowserDevelopmentDistribution --no-daemon

# --- Stage 2: Backend (JRE + Ktor) ---
FROM eclipse-temurin:21-jre-alpine AS backend
WORKDIR /app
COPY --from=builder /app/backend/build/install/backend ./
EXPOSE 8080
ENTRYPOINT ["bin/backend"]

# --- Stage 3: Frontend (nginx + owner-web static files) ---
FROM nginx:alpine AS frontend
COPY --from=builder /app/owner-web/build/dist/wasmJs/developmentExecutable /usr/share/nginx/html
COPY docker/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
