FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app
RUN useradd --system --create-home --home-dir /app lims
COPY --from=build /workspace/target/lims4wuxi-backend-0.1.0-SNAPSHOT.jar /app/app.jar
USER lims

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
