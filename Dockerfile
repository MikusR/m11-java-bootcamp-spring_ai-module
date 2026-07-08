FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN ./mvnw -B dependency:go-offline

COPY src src
RUN ./mvnw -B package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

RUN mkdir -p /data
COPY --from=build /workspace/target/*.jar /app/app.jar

ENV SQLITE_DB_PATH=/data/chatbot.db
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
