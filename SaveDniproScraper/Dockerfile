# OpenJDK 19 as the base image
FROM openjdk:19-jdk-slim

WORKDIR /app

# Download the latest release of SaveDniproScraper.jar from GitHub
ADD https://github.com/oleksandrvolovyk/ua-eco-aggregator-public/releases/latest/download/SaveDniproScraper.jar /app/SaveDniproScraper.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "SaveDniproScraper.jar"]