FROM eclipse-temurin:21-jre
ARG JAR_FILE=target/*.jar
WORKDIR /app
COPY ${JAR_FILE} app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
