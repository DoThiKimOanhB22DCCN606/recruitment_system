# Stage 1: Build — multi-module aware
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy all pom.xml files so Maven reactor can resolve full module tree
COPY pom.xml .
COPY shared/rms-common/pom.xml ./shared/rms-common/
COPY gateway/api-gateway/pom.xml ./gateway/api-gateway/
COPY services/iam-service/pom.xml ./services/iam-service/
COPY services/candidate-service/pom.xml ./services/candidate-service/
COPY services/job-service/pom.xml ./services/job-service/
COPY services/ats-service/pom.xml ./services/ats-service/
COPY services/interview-service/pom.xml ./services/interview-service/
COPY services/offer-service/pom.xml ./services/offer-service/
COPY services/company-service/pom.xml ./services/company-service/

# Copy sources for shared and offer
ARG SERVICE_MODULE=services/offer-service
COPY shared/rms-common/src ./shared/rms-common/src
COPY ${SERVICE_MODULE}/src ./${SERVICE_MODULE}/src

# Build module
RUN mvn clean package -pl shared/rms-common,${SERVICE_MODULE} -am -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
ARG SERVICE_MODULE=services/offer-service
COPY --from=build /app/${SERVICE_MODULE}/target/*.jar app.jar
ARG EXPOSE_PORT=8086
EXPOSE ${EXPOSE_PORT}
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
