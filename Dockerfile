FROM maven:3.5.0-jdk-7
MAINTAINER David Esner <esnerda@gmail.com>

ENV APP_VERSION 1.1.5

WORKDIR /home

RUN export MAVEN_OPTS="-XX:MaxRAM=500m -Xmx:256m -Xms:256m -Xss:100 -XX:MaxPermSize=128M"

RUN git clone https://github.com/davidesner/keboola-adform-masterdata-extractor ./
RUN mvn -q compile

ENTRYPOINT java -jar target/KBC_AdForm_Masterdata_extractor-1.1.5-jar-with-dependencies.jar /data