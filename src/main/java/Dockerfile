FROM openjdk:17-jdk-slim

WORKDIR /app

COPY . .

RUN chmod +x mvnw || true

RUN ./mvnw clean install || mvn clean install

EXPOSE 8080

CMD ["java", "-jar", "target/image-0.0.1-SNAPSHOT.jar"]