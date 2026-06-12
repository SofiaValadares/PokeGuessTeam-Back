FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app

COPY pom.xml .
RUN mvn -q dependency:go-offline -DskipTests

COPY src src
RUN mvn -q package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN apk add --no-cache nginx gettext wget \
    && addgroup -S app && adduser -S app -G app

COPY --from=build /app/target/pokeguessteam-*.jar app.jar
COPY docker/nginx.conf.template /app/nginx.conf.template
COPY docker/start.sh /app/start.sh
RUN chmod +x /app/start.sh

EXPOSE 8080

ENTRYPOINT ["/app/start.sh"]
