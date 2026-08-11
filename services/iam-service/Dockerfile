# Stage 1: Build — multi-module aware
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# Copy parent POM first (layer cache for dependency resolution)
COPY pom.xml .
COPY shared/rms-common/pom.xml ./shared/rms-common/
# ARG injected by docker-compose build args (e.g. services/iam-service)
ARG SERVICE_MODULE=services/iam-service
COPY ${SERVICE_MODULE}/pom.xml ./${SERVICE_MODULE}/
# Download all dependencies as a separate cached layer
RUN mvn dependency:go-offline -pl shared/rms-common,${SERVICE_MODULE} -am -q
# Copy sources
COPY shared/rms-common/src ./shared/rms-common/src
COPY ${SERVICE_MODULE}/src ./${SERVICE_MODULE}/src
# Build only this module and its upstream dependencies
RUN mvn clean package -pl shared/rms-common,${SERVICE_MODULE} -am -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
ARG SERVICE_MODULE=services/iam-service
COPY --from=build /app/${SERVICE_MODULE}/target/*.jar app.jar
ARG EXPOSE_PORT=8081
EXPOSE ${EXPOSE_PORT}
# Enable container-aware memory sizing
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
