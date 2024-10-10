FROM openjdk:21-slim

ENV JAVA_HOME=/usr/local/openjdk-21
ENV PATH="$JAVA_HOME/bin:$PATH"

WORKDIR /app

COPY target/server.jar /app/server.jar

ENTRYPOINT ["java", "-jar", "server.jar"]
CMD ["SECRET=Velocity-Server-Secret"]