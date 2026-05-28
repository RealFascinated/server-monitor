FROM eclipse-temurin:25-jdk-alpine AS build

WORKDIR /build

COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle.kts settings.gradle.kts ./

RUN chmod +x gradlew && ./gradlew dependencies --no-daemon -q || true

COPY src/ src/

RUN ./gradlew bootJar --no-daemon -q

FROM eclipse-temurin:25-jre-alpine

WORKDIR /home/container

COPY --from=build /build/build/libs/monitor-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 80
ENV PORT=80
ENV ENVIRONMENT=production

CMD ["java", "-XX:MaxRAMPercentage=75.0", "-Djava.awt.headless=true", "-jar", "app.jar"]
