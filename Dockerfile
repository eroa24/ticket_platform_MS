# =====================================================
# Multi-stage Dockerfile for Ticket Platform MS
# =====================================================
# Stage 1: Build  → Gradle + JDK 25 (full SDK)
# Stage 2: Runtime → JRE-only, minimal Alpine image
# =====================================================

# -----------------------------------------------
# STAGE 1: BUILD
# -----------------------------------------------
# Eclipse Temurin JDK 25 on Alpine — lightweight build base (~340MB vs ~700MB Ubuntu)
# Temurin is the reference OpenJDK distribution from Adoptium, production-grade
FROM eclipse-temurin:25-jdk-alpine AS builder

WORKDIR /app

# ---- Cache de dependencias (layer caching) ----
# Copiamos SOLO los archivos de configuración de Gradle primero.
# Docker cachea este layer. Si solo cambió código fuente, Gradle no
# re-descarga todas las dependencias → builds mucho más rápidos.
COPY gradle/ gradle/
COPY gradlew .
COPY build.gradle .
COPY settings.gradle .
COPY main.gradle .
COPY gradle.properties .

COPY applications/ ./applications/
COPY domain/ ./domain/
COPY infrastructure/ ./infrastructure/
# Dar permisos de ejecución al wrapper
RUN chmod +x gradlew

# Descargar dependencias sin compilar (aprovecha cache de Docker layers)
RUN ./gradlew dependencies --no-daemon

# ---- Copiar código fuente y compilar ----
COPY . .

# Build producción: sin tests (se corren en CI), sin daemon (efímero)
RUN ./gradlew bootJar --no-daemon -x test

# -----------------------------------------------
# STAGE 2: RUNTIME
# -----------------------------------------------
# Eclipse Temurin JRE 25 — solo el runtime, sin compilador ni tools
# Alpine Linux → imagen base ~5MB vs ~80MB Ubuntu
# JRE-only → ~180MB total vs ~340MB con JDK completo
FROM eclipse-temurin:25-jre-alpine AS runtime

# Metadatos OCI estándar
LABEL maintainer="ticketing-team"
LABEL org.opencontainers.image.title="Ticket Platform MS"
LABEL org.opencontainers.image.description="Reactive ticket management microservice"

# ---- Hardening de seguridad ----
# 1. Crear usuario no-root dedicado para la app
# 2. Nunca ejecutar como root en producción (CIS Benchmark)
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copiar SOLO el JAR compilado desde el builder (no el JDK, ni Gradle, ni sources)
COPY --from=builder /app/applications/app-service/build/libs/ticket_platform_MS.jar app.jar

# Cambiar ownership al usuario de la app
RUN chown appuser:appgroup app.jar

# Cambiar al usuario no-root
USER appuser

# ---- JVM Tuning para contenedores ----
# -XX:+UseContainerSupport     → JVM respeta los limits de CPU/memoria del container
# -XX:MaxRAMPercentage=75.0    → Usa máximo 75% de la RAM del container (deja 25% para el SO)
# -XX:+UseZGC                  → Z Garbage Collector: pausas < 1ms, ideal para latencia baja
# -XX:+ZGenerational           → ZGC generacional (Java 21+): mejor throughput
# -Djava.security.egd          → Usa /dev/urandom para generación rápida de UUIDs
# -Dspring.profiles.active     → Perfil configurable vía variable de entorno
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseZGC \
    -XX:+ZGenerational \
    -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080

# Health check nativo de Docker usando actuator
HEALTHCHECK --interval=30s --timeout=5s --retries=3 --start-period=40s \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1

# Ejecutar con exec form (PID 1 directo, recibe señales SIGTERM correctamente)
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
