# Stage 1: Build the JAR
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Stage 2: Run the JAR
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# Matches the port shown in your ACTIVITY_SERVICE_URL
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]