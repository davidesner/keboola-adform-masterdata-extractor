FROM maven:3.5.0-jdk-8
MAINTAINER David Esner <esnerda@gmail.com>

ENV APP_VERSION 1.1.5

COPY . /code/
ENV MAVEN_OPTS="-XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap -Xmx512m"
ENV JAVA_OPTS="-XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap -Xmx512m"
WORKDIR /code/
RUN mvn compile

ENTRYPOINT mvn -q exec:java -Dexec.args=/data