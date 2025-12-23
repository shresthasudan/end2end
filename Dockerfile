# Use JDK 21 (Eclipse Temurin is lightweight and standard)
FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

# Copy the built jar from target
COPY target/demo-app-1.0.0.jar app.jar

# Expose port
EXPOSE 8080

# Run the app
ENTRYPOINT ["java", "-jar", "app.jar"]