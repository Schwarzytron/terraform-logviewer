FROM gradle:8.7-jdk21-alpine AS build

RUN apk update && apk add --no-cache \
    gcompat \
    protobuf \
    protobuf-dev

WORKDIR /app
COPY build.gradle.kts .
COPY src ./src

RUN gradle build -x test

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]