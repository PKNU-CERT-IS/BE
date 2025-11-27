# Use official OpenJDK base image
FROM eclipse-temurin:21-jdk-jammy

# Install curl and unzip
RUN apt-get update && apt-get install -y curl unzip

# Set working directory
WORKDIR /backend

# Copy Gradle executable
COPY gradlew .
COPY gradle gradle

# Copy build configuration
COPY build.gradle .
COPY settings.gradle .

# Make gradlew executable
RUN chmod +x ./gradlew

# Manually download and install Gradle
RUN mkdir -p /root/.gradle/wrapper/dists/gradle-8.12.1-bin/abcdefg && \
    curl -L --connect-timeout 60 --retry 5 --retry-delay 5 https://services.gradle.org/distributions/gradle-8.12.1-bin.zip -o /tmp/gradle.zip && \
    unzip /tmp/gradle.zip -d /root/.gradle/wrapper/dists/gradle-8.12.1-bin/abcdefg && \
    rm /tmp/gradle.zip

# Copy source code (do this first to avoid redownloading dependencies)
COPY src ./src

# Build the application
RUN ./gradlew build -x test --no-daemon

# Expose port
EXPOSE 8080

# Run the jar file
ENTRYPOINT ["java", "-jar", "/backend/build/libs/certis-1.0-SNAPSHOT.jar"]