FROM openjdk:11-jre-slim

RUN groupadd --gid 999 appuser && \
  useradd --system --uid 999 --gid appuser appuser

COPY build/libs/xray-experiment-1.0-SNAPSHOT.jar /app/app.jar

WORKDIR /app

RUN chmod +x app.jar

USER appuser

EXPOSE 8080

CMD ./app.jar
