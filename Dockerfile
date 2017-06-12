FROM maven:3.5.0-jdk-7
MAINTAINER David Esner <esnerda@gmail.com>

ENV APP_VERSION 1.1.5

WORKDIR /home

RUN export MAVEN_OPTS="-XX:MaxRAM=500m"

RUN git clone https://github.com/davidesner/keboola-adform-masterdata-extractor ./
RUN mvn -q compile

ENTRYPOINT mvn exec:java -Dexec.args=/data