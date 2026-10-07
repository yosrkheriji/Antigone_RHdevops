FROM eclipse-temurin:17-jdk-alpine AS build
RUN apk add --no-cache curl
WORKDIR /app
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B
COPY src src
RUN ./mvnw package -DskipTests -B

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
COPY entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh && \
    mkdir -p uploads/justificatifs uploads/images uploads/documents uploads/agent
COPY uploads/agent/AgentDesktop-Setup.exe uploads/agent/
RUN addgroup -S appgroup && adduser -S appuser -G appgroup \
    && chown -R appuser:appgroup /app
USER appuser
EXPOSE 8080
ENTRYPOINT ["/app/entrypoint.sh"]
