FROM openjdk:17.0.2-jdk

ENV JVM_OPT -Xms250m -Xmx250m

COPY build/libs/*.jar MaaBackendCenter.jar
ENTRYPOINT /usr/bin/java -Djava.security.egd=file:/dev/./urandom ${JVM_OPT} -jar MaaBackendCenter.jar