FROM 18-jdk-oraclelinux8

ENV JVM_OPT -Xms250m -Xmx250m

COPY ../build/libs/MaaBackendCenter.jar MaaBackendCenter.jar
ENTRYPOINT /usr/bin/java -Djava.security.egd=file:/dev/./urandom ${JVM_OPT} -jar MaaBackendCenter.jar