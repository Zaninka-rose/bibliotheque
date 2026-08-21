# syntax=docker/dockerfile:1

# ---------- Étape 1 : compilation ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Télécharge les dépendances (mis en cache par BuildKit pour les builds suivants)
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp dependency:go-offline

COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp -DskipTests package

# ---------- Étape 2 : runtime ----------
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/target/bibliotheque-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
