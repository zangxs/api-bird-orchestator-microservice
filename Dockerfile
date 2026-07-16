# Multi-stage build — no mvnw wrapper checked into this repo, so the build stage brings its own Maven.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Cache dependencies in their own layer before copying source.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /build/target/*.jar app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
