# OpenJDK 19 as the base image
FROM openjdk:19-jdk-slim

WORKDIR /app

# Download the latest release of ScraperAPI.jar from GitHub
ADD https://github.com/oleksandrvolovyk/ua-eco-aggregator-public/releases/latest/download/ScraperAPI.jar /app/ScraperAPI.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "ScraperAPI.jar"]