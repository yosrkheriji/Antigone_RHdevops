# syntax=docker/dockerfile:1
# ---------- Etape 1 : compilation Maven (JDK 17) ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
# pom.xml seul d'abord : la couche des dependances reste en cache tant que le pom ne change pas.
COPY pom.xml ./
RUN mvn -B -ntp dependency:go-offline
COPY src src
# Les tests sont executes par le pipeline CI (job backend), pas pendant le build de l'image.
RUN mvn -B -ntp package -DskipTests

# ---------- Etape 2 : image d'execution (JRE seul) ----------
FROM eclipse-temurin:17-jre-alpine
LABEL org.opencontainers.image.title="antigone-backend" \
      org.opencontainers.image.description="Antigone 360 - API Spring Boot"
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
COPY entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh && \
    mkdir -p uploads/justificatifs uploads/images uploads/documents uploads/agent
COPY uploads/agent/AgentDesktop-Setup.exe uploads/agent/
RUN addgroup -S appgroup && adduser -S appuser -G appgroup \
    && chown -R appuser:appgroup /app
USER appuser
# La JVM dimensionne son tas d'apres la memoire allouee au conteneur.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080
# /v3/api-docs est public (SecurityConfig) : repond 200 des que le contexte Spring est pret.
HEALTHCHECK --interval=15s --timeout=5s --start-period=120s --retries=5 \
  CMD wget -qO /dev/null http://127.0.0.1:8080/v3/api-docs || exit 1
ENTRYPOINT ["/app/entrypoint.sh"]
