FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /app
COPY pom.xml .
RUN mvn --batch-mode dependency:go-offline
COPY src ./src
RUN mvn --batch-mode package

FROM eclipse-temurin:24-jre-alpine

WORKDIR /app
RUN addgroup -S gymius && adduser -S gymius -G gymius
COPY --from=build /app/target/app.jar app.jar
RUN chown gymius:gymius app.jar

USER gymius
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
