FROM gradle:9.7.1-jdk25-alpine AS build
WORKDIR /workdir
ENV GRADLE_USER_HOME=/gradle-cache

COPY build.gradle.kts settings.gradle.kts ./
RUN --mount=type=cache,target=/gradle-cache \
    gradle --no-daemon dependencies --configuration runtimeClasspath

COPY src ./src
RUN --mount=type=cache,target=/gradle-cache \
    gradle bootJar --no-daemon -x test

FROM gcr.io/distroless/java25-debian13:nonroot
WORKDIR /workdir

COPY --from=build /workdir/build/libs/app.jar app.jar

EXPOSE 8081
USER 10001:10001
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
