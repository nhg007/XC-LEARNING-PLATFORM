FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

RUN addgroup -S xc && adduser -S xc -G xc

COPY backend/xc-api/target/xc-api-*.jar /app/app.jar
COPY database/migrations /app/database/migrations

ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080

USER xc

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
