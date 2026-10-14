FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /workspace

COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

RUN apk add --no-cache wget && addgroup -S app && adduser -S -G app app
COPY --from=build --chown=app:app /workspace/target/task-manager-0.0.1-SNAPSHOT.jar /app/app.jar

USER app
EXPOSE 8085
ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
