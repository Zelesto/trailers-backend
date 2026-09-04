# ============================================================================
# OPTIMIZED DOCKERFILE FOR SPRING BOOT
# ============================================================================

# Stage 1: Build with dependency caching
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# ✅ Copy only pom.xml first to cache dependencies
COPY pom.xml ./
RUN mvn dependency:go-offline -B

# ✅ Copy source and build
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy the built jar
COPY --from=build --chown=spring:spring /app/target/*.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
