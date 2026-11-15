FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app

COPY pom.xml .
RUN mvn -q dependency:go-offline -DskipTests

COPY src src
RUN mvn -q package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app \
    && mkdir -p /tmp/logs \
    && chown -R app:app /tmp/logs

COPY --from=build --chown=app:app /app/target/pokeguessteam-*.jar app.jar

USER app

# Relativo a /app falha (user sem permissão); /tmp é gravável no container.
ENV LOG_PATH=/tmp/logs

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
