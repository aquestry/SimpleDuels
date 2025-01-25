FROM openjdk:21-slim
ENV JAVA_HOME=/usr/local/openjdk-21
ENV PATH="$JAVA_HOME/bin:$PATH"
WORKDIR /app
COPY server.jar /app/server.jar
CMD ["java", "-jar", "server.jar"]