# OpenJDK 19 as the base image
FROM openjdk:19-jdk-slim

WORKDIR /app

# Download the latest release of MeteoGovUaScraper.jar from GitHub
ADD https://github.com/oleksandrvolovyk/ua-eco-aggregator-public/releases/latest/download/MeteoGovUaScraper.jar /app/MeteoGovUaScraper.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "MeteoGovUaScraper.jar"]