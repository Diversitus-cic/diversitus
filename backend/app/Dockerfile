# Stage 1: Build the application
FROM gradle:8.6-jdk17-alpine AS builder
WORKDIR /home/gradle/src
COPY --chown=gradle:gradle . .
RUN gradle :backend:app:build fatJar --no-daemon

# Stage 2: Create the final, smaller image
FROM amazoncorretto:17-alpine-jdk
WORKDIR /app

COPY --from=builder /home/gradle/src/backend/app/build/libs/*.jar ./app.jar

EXPOSE 8080
CMD ["java", "-jar", "app.jar"]