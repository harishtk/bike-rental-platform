FROM gradle:9.7.1-jdk21 AS builder

WORKDIR /app

COPY build.gradle settings.gradle jacoco.gradle ./

# Download the dependencies
RUN gradle dependencies --no-daemon

COPY src ./src

# Build the application
RUN gradle bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine AS runner

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar ./app.jar

EXPOSE 8001

ENTRYPOINT [ "java", "-jar", "app.jar" ]