# Stage 1: Build — multi-module aware
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY shared/rms-common/pom.xml ./shared/rms-common/
ARG SERVICE_MODULE=gateway/api-gateway
COPY ${SERVICE_MODULE}/pom.xml ./${SERVICE_MODULE}/
RUN mvn dependency:go-offline -pl shared/rms-common,${SERVICE_MODULE} -am -q
COPY shared/rms-common/src ./shared/rms-common/src
COPY ${SERVICE_MODULE}/src ./${SERVICE_MODULE}/src
RUN mvn clean package -pl shared/rms-common,${SERVICE_MODULE} -am -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
ARG SERVICE_MODULE=gateway/api-gateway
COPY --from=build /app/${SERVICE_MODULE}/target/*.jar app.jar
ARG EXPOSE_PORT=8080
EXPOSE ${EXPOSE_PORT}
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
