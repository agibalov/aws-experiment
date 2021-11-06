FROM ubuntu as builder

RUN apt-get update && apt-get install -y curl unzip

RUN mkdir /xray && \
  curl -L https://github.com/aws/aws-xray-java-agent/releases/download/v2.9.1/xray-agent.zip \
    --output /xray/xray-agent.zip && \
  unzip /xray/xray-agent.zip -d /xray

FROM openjdk:11-jre-slim

RUN groupadd --gid 999 appuser && \
  useradd --system --uid 999 --gid appuser appuser

COPY build/libs/xray-experiment-1.0-SNAPSHOT.jar /app/app.jar
COPY --from=builder /xray/disco /xray

WORKDIR /app

RUN chmod +x app.jar

USER appuser

EXPOSE 8080

#CMD ./app.jar
ENTRYPOINT [ "sh", "-c", "java -Dlogging.level.com.amazonaws.xray=DEBUG -javaagent:/xray/disco-java-agent.jar=pluginPath=/xray/disco-plugins -jar app.jar" ]
