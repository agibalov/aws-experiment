FROM openjdk:8-jre
ARG jarFilename
ARG tag
ENV TAG ${tag}
COPY build/libs/${jarFilename} /tmp/app.jar
ENTRYPOINT exec java -jar /tmp/app.jar
EXPOSE 8080
