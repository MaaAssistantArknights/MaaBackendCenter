FROM gradle:jdk17-alpine AS builder
WORKDIR /app
COPY . .
RUN gradle build -x test --no-daemon --stacktrace

FROM openjdk:jre-alpine as runner
WORKDIR /app
COPY --from=builder /app/build/libs/MaaBackendCenter*.jar /MaaBackendCenter.jar
EXPOSE 8888
ENTRYPOINT ["java", "-jar", "/MaaBackendCenter.jar"]
