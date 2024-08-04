FROM openjdk:17-slim
FROM maven:latest
COPY . /app
WORKDIR /app
RUN mvn clean package -DskipTests
ENTRYPOINT ["java", "-jar","/app/target/GveraBot-0.0.1-SNAPSHOT.jar"]