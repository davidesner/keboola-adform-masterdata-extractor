FROM maven:3.5.0-jdk-8
MAINTAINER David Esner <esnerda@gmail.com>

ENV APP_VERSION 1.1.5

WORKDIR /home

ENV MAVEN_OPTS="-Xmx256m -Xms256m -Xss100m -XX:MaxPermSize=128m"
ENV JAVA_OPTS="-Xmx256m -Xms256m -Xss100m -XX:MaxPermSize=128m"
RUN git clone https://github.com/davidesner/keboola-adform-masterdata-extractor ./
RUN mvn -q install

ENTRYPOINT java -jar target/KBC_AdForm_Masterdata_extractor-1.1.5-jar-with-dependencies.jar /data