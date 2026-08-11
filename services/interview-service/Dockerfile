FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml pom.xml
COPY gateway/pom.xml gateway/pom.xml
COPY services/ats-service/pom.xml services/ats-service/pom.xml
COPY services/interview-service/pom.xml services/interview-service/pom.xml
RUN mvn -pl services/interview-service -am dependency:go-offline
COPY services/interview-service/src services/interview-service/src
RUN mvn -pl services/interview-service -am package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /workspace/services/interview-service/target/interview-service-*.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]
