# syntax=docker/dockerfile:1
# Dockerfile generico: gera a imagem de qualquer modulo do projeto.
#   docker build --build-arg MODULE=pecas-service -t pecas-service .
# O docker-compose.yml usa este arquivo para os 6 servicos.

# ---------- Build ----------
FROM maven:3.9-eclipse-temurin-21 AS build
ARG MODULE
WORKDIR /src
COPY ${MODULE}/ ${MODULE}/
# O cache do ~/.m2 e compartilhado entre os builds dos modulos (baixa as dependencias uma vez so)
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    mvn -q -f ${MODULE}/pom.xml package -DskipTests \
 && cp ${MODULE}/target/${MODULE}-0.0.1-SNAPSHOT.jar /app.jar

# ---------- Runtime ----------
FROM eclipse-temurin:21-jre
# curl e usado pelos healthchecks do docker-compose
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /app.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
