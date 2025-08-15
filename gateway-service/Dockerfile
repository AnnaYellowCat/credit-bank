FROM maven:3.9.7-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src /app/src
RUN mvn package -DskipTests

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/target/gateway-service-*.jar /app/app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]