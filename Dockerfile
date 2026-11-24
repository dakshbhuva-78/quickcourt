# ---------- Stage 1: build the JAR with Maven ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---------- Stage 2: small runtime image (JRE only) ----------
FROM eclipse-temurin:17-jre
WORKDIR /app

# run as a normal user, not root (security best practice)
RUN useradd --system --uid 1001 quickcourt \
    && mkdir -p /app/logs /app/exports /app/reports \
    && chown -R quickcourt:quickcourt /app
COPY --from=build /app/target/*.jar /app/app.jar

USER quickcourt
ENV TZ=Asia/Kolkata
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
