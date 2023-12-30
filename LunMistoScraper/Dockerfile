# OpenJDK 19 as the base image
FROM openjdk:19-jdk-slim

WORKDIR /app

# Download the latest release of LunMistoScraper.jar from GitHub
ADD https://github.com/oleksandrvolovyk/ua-eco-aggregator-public/releases/latest/download/LunMistoScraper.jar /app/LunMistoScraper.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "LunMistoScraper.jar"]