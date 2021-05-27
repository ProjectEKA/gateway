FROM adoptopenjdk/openjdk11:jre-11.0.8_10-alpine
VOLUME /tmp
COPY build/libs/* app.jar
EXPOSE 8000
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app.jar"]