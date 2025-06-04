FROM maven:3.9-eclipse-temurin-21

WORKDIR /app

# Copy POM first
COPY pom.xml .

# Pre-download ALL dependencies, including plugin dependencies and spring-boot-maven-plugin
RUN mvn dependency:go-offline -B && \
    mvn help:evaluate -Dexpression=project.artifactId -q -DforceStdout && \
    mvn org.springframework.boot:spring-boot-maven-plugin:help -B && \
    mvn compiler:help -B

# Copy the source code
COPY src ./src

# Package once to ensure all possible dependencies are resolved
RUN mvn package -DskipTests

# Create Maven settings file to run in offline mode
RUN echo '<settings><offline>true</offline></settings>' > /root/.m2/settings.xml

# Expose the application port
EXPOSE 8081

# Set environment variables
ENV JAVA_OPTS=""

# Run in offline mode to prevent downloads
ENTRYPOINT ["sh", "-c", "mvn -o spring-boot:run $JAVA_OPTS"]
