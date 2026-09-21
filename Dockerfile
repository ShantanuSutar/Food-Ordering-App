# syntax=docker/dockerfile:1

FROM eclipse-temurin:17-jdk-jammy AS build

WORKDIR /workspace

# Resolve dependencies before copying source so this layer remains cached when
# only application code changes.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -ntp dependency:go-offline

COPY src/ src/
RUN ./mvnw -B -ntp clean package -DskipTests

FROM eclipse-temurin:17-jre-jammy AS runtime

RUN groupadd --system dinehub \
    && useradd --system --gid dinehub --home-dir /app --shell /usr/sbin/nologin dinehub

WORKDIR /app

COPY --from=build --chown=dinehub:dinehub /workspace/target/*.jar app.jar

USER dinehub

EXPOSE 9090

# JAVA_TOOL_OPTIONS can be supplied by the deployment platform when JVM tuning
# is needed without changing this image.
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
