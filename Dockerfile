FROM amazoncorretto:17-alpine as runner
WORKDIR /app
COPY ./build/libs/MaaBackendCenter*.jar /MaaBackendCenter.jar
EXPOSE 7000-9000
ENTRYPOINT ["java", "-jar", "/MaaBackendCenter.jar"]
