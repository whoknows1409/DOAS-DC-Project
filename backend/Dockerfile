FROM openjdk:17-jdk-slim

WORKDIR /app

COPY target/distributed-auction-system-1.0.0.jar app.jar

EXPOSE 8080 1100

CMD ["java", "-jar", "app.jar"]