# OpenJDK 19 as the base image
FROM openjdk:19-jdk-slim

WORKDIR /app

# Download the latest release of AdminAPI.jar from GitHub
ADD https://github.com/oleksandrvolovyk/ua-eco-aggregator-public/releases/latest/download/AdminAPI.jar /app/AdminAPI.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "AdminAPI.jar"]