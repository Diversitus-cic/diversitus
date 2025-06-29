
# Stage 1: Build the application
FROM gradle:8.6-jdk17-alpine AS builder
WORKDIR /home/gradle/src

# Copy build configuration files first to leverage Docker layer caching
COPY build.gradle.kts settings.gradle.kts gradlew ./
COPY gradle/ ./gradle/
COPY backend/app/build.gradle.kts ./backend/app/

# Download dependencies. This layer is cached as long as build files don't change.
RUN gradle :backend:app:dependencies --no-daemon

# Copy the rest of the source code and build the application
COPY --chown=gradle:gradle . .
RUN gradle :backend:app:shadowJar --no-daemon --rerun-tasks

# Stage 2: Create the final Lambda-compatible image
FROM public.ecr.aws/lambda/java:17

# Copy the shadow JAR (fat JAR with all dependencies)
COPY --from=builder /home/gradle/src/backend/app/build/libs/*-all.jar ${LAMBDA_TASK_ROOT}/app.jar

# Set the CMD to your Lambda handler class and method
# Format: package.ClassName::methodName
CMD ["com.diversitus.LambdaHandler::handleRequest"]