FROM centos:centos7
MAINTAINER David Esner <esnerda@gmail.com>

ENV APP_VERSION 1.1.4

RUN yum -y update && \
	yum -y install \
		epel-release \
		git \
		tar \
		&& \
	yum clean all


RUN yum -y install wget
RUN wget http://repos.fedorapeople.org/repos/dchen/apache-maven/epel-apache-maven.repo -O /etc/yum.repos.d/epel-apache-maven.repo
RUN yum -y install apache-maven

WORKDIR /home

ENV JAVA_HOME /usr/lib/jvm/jre-1.7.0
RUN --branch v1.1.4 https://github.com/davidesner/keboola-adform-masterdata-extractor ./  
RUN mvn compile

ENTRYPOINT mvn exec:java -Dexec.args=/data  