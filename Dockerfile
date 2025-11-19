
FROM maven:3.9-eclipse-temurin-21 AS builder
LABEL authors="janju"


WORKDIR /app

COPY pom.xml .

RUN mvn dependency:go-offline -B

COPY src ./src
COPY src/main/resources/ ./src/main/resources/
COPY src/main/resources/agent_profiles ./src/main/resources/agent_profiles
COPY src/main/resources/bill_agent ./src/main/resources/bill_agent
COPY src/main/resources/tech_agent ./src/main/resources/tech_agent
COPY src/main/resources/docs ./src/main/resources/docs
COPY src/main/resources/single_query ./src/main/resources/single_query
COPY src/main/resources/static ./src/main/resources/static

RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
