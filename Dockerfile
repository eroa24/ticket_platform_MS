# =====================================================
# Ticket Platform MS
# =====================================================

# -----------------------------------------------
# STAGE 1: BUILD
# -----------------------------------------------
FROM eclipse-temurin:25-jdk-alpine AS builder

WORKDIR /app

COPY gradle/ gradle/
COPY gradlew .
COPY build.gradle .
COPY settings.gradle .
COPY main.gradle .
COPY gradle.properties .

COPY applications/ ./applications/
COPY domain/ ./domain/
COPY infrastructure/ ./infrastructure/

RUN chmod +x gradlew

RUN ./gradlew dependencies --no-daemon

# ---- Copiar código fuente y compilar ----
COPY . .

RUN ./gradlew bootJar --no-daemon -x test

# -----------------------------------------------
# STAGE 2: RUNTIME
# -----------------------------------------------
FROM eclipse-temurin:25-jre-alpine AS runtime

LABEL maintainer="ticketing-team"
LABEL org.opencontainers.image.title="Ticket Platform MS"
LABEL org.opencontainers.image.description="Reactive ticket management microservice"

# ---- seguridad ----
# 1. Crear usuario no-root dedicado para la app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

COPY --from=builder /app/applications/app-service/build/libs/ticket_platform_MS.jar app.jar

RUN chown appuser:appgroup app.jar

# Cambiar al usuario no-root
USER appuser

# Optimiza el uso de recursos en entornos de contenedores, 
# asegurando que la aplicación respete los límites de memoria asignados
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseZGC \
    -XX:+ZGenerational \
    -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=5s --retries=3 --start-period=40s \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
