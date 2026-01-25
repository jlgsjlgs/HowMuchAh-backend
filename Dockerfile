# Build stage
FROM amazoncorretto:25-alpine AS builder
RUN apk add --no-cache maven
WORKDIR /app
COPY pom.xml ./
COPY .mvn .mvn
COPY mvnw ./
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline -B
COPY src ./src
RUN ./mvnw clean package -DskipTests

# CDS stage - Create Class Data Sharing archive
FROM amazoncorretto:25-alpine AS cds
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

# Generate CDS archive
RUN java -XX:ArchiveClassesAtExit=app.jsa \
    -Dspring.context.exit=onRefresh \
    -jar app.jar || true

# Runtime stage
FROM amazoncorretto:25-alpine
RUN apk add --no-cache wget
RUN addgroup -S spring && adduser -S spring -G spring
WORKDIR /app

# Copy jar and CDS archive
COPY --from=builder /app/target/*.jar app.jar
COPY --from=cds /app/app.jsa app.jsa
RUN chown -R spring:spring /app
USER spring:spring

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# OPTIMIZED ENTRYPOINT with TieredCompilation + CDS
ENTRYPOINT ["java", \
    "-XX:+UseSerialGC", \
    "-XX:SharedArchiveFile=app.jsa", \
    "-XX:+TieredCompilation", \
    "-XX:TieredStopAtLevel=1", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:InitialRAMPercentage=50.0", \
    "-Xss512k", \
    "-XX:MaxMetaspaceSize=150m", \
    "-XX:ReservedCodeCacheSize=32m", \
    "-XX:MaxHeapFreeRatio=40", \
    "-XX:MinHeapFreeRatio=20", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", \
    "app.jar"]