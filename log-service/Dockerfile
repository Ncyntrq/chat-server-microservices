# syntax=docker/dockerfile:1.6
# Multi-stage build cho log-service.
# Build context PHẢI là project root (vì log-service phụ thuộc common-lib).

# ---------- Stage 1: Build ----------
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /build

# Cache layer Maven Wrapper + parent POM
COPY mvnw mvnw.cmd pom.xml ./
COPY .mvn .mvn

# Module dependency
COPY common-lib common-lib
COPY log-service log-service

# Build executable jar (skip test để image build nhanh — test đã chạy ở CI/local)
RUN ./mvnw -pl log-service -am clean package -DskipTests

# ---------- Stage 2: Runtime ----------
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

COPY --from=build /build/log-service/target/log-service-1.0.0-SNAPSHOT.jar app.jar

ENV TZ=Asia/Ho_Chi_Minh \
    JAVA_OPTS=""

EXPOSE 8084

# Logs ghi vào /app/logs/chat_log.txt — mount volume khi chạy compose
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
