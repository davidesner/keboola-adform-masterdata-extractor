FROM keboola/base

MAINTAINER David Esner <esnerd@gmail.com>

ENV APP_VERSION 1.0.0
RUN yum -y install wget

RUN wget http://repos.fedorapeople.org/repos/dchen/apache-maven/epel-apache-maven.repo -O /etc/yum.repos.d/epel-apache-maven.repo
RUN yum -y install apache-maven
RUN yum -y install git-all
WORKDIR /home

RUN git clone https://github.com/davidesner/keboola-adform-masterdata-extractor ./  
RUN mvn compile

ENTRYPOINT mvn exec:java -Dexec.args=/data  