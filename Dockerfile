# Dockerfile for the generating the webapp image
#
# To build:
#
# docker build -t docker.lib.umd.edu/fcrepo-webapp:<VERSION> -f Dockerfile .
#
# where <VERSION> is the Docker image version to create.
FROM maven:3.8.6-eclipse-temurin-11 AS compile

ENV SOURCE_DIR /opt/umd-fcrepo-webapp
COPY src $SOURCE_DIR/src
COPY pom.xml $SOURCE_DIR
WORKDIR $SOURCE_DIR
RUN mvn package -DwarFileName=umd-fcrepo-webapp

FROM tomcat:8.5.83-jdk8-temurin-jammy

# default context path is the root, making the full URL e.g. http://localhost:8080/
ENV CONTEXT_PATH=""
# default heap size is 2 GB
ENV TOMCAT_HEAP=2048m

RUN mkdir -p /opt/umd-fcrepo-webapp
COPY --from=compile /opt/umd-fcrepo-webapp/target/umd-fcrepo-webapp.war /opt/umd-fcrepo-webapp/
COPY setenv.sh /usr/local/tomcat/bin/
COPY server.xml /usr/local/tomcat/conf/

VOLUME /var/umd-fcrepo-webapp
# for the store-and-forward broker
VOLUME /var/activemq
