FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
ARG SERVICE_MODULE=services/offer-service
COPY ${SERVICE_MODULE}/target/*-SNAPSHOT.jar app.jar
ARG EXPOSE_PORT=8086
EXPOSE ${EXPOSE_PORT}
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
