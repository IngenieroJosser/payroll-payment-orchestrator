# syntax=docker/dockerfile:1.7
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod 0555 mvnw && ./mvnw -B -ntp -DskipTests dependency:go-offline
COPY src/ src/
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -ntp clean package

FROM eclipse-temurin:21-jre-jammy AS runtime
RUN groupadd --system --gid 10001 app && useradd --system --uid 10001 --gid app --home-dir /app app
WORKDIR /app
COPY --from=build --chown=app:app /workspace/target/payroll-payment-orchestrator-*.jar app.jar
USER 10001:10001
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError -Dfile.encoding=UTF-8"
ENTRYPOINT ["java","-jar","/app/app.jar"]
