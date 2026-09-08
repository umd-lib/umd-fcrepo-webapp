# Dockerfile for the generating the webapp image
#
# To build:
#
# docker build -t docker.lib.umd.edu/fcrepo-webapp:<VERSION> -f Dockerfile .
#
# where <VERSION> is the Docker image version to create.
FROM maven:3-eclipse-temurin-25 AS compile

ENV SOURCE_DIR=/opt/umd-fcrepo-webapp
COPY src $SOURCE_DIR/src
COPY pom.xml $SOURCE_DIR
WORKDIR $SOURCE_DIR
RUN mvn package -DwarFileName=umd-fcrepo-webapp

FROM tomcat:10.1.57-jdk25-temurin

# default context path is "/fcrepo", making the full URL e.g. http://localhost:8080/fcrepo
ENV CONTEXT_PATH="/fcrepo"

RUN mkdir -p /opt/umd-fcrepo-webapp
COPY --from=compile /opt/umd-fcrepo-webapp/target/umd-fcrepo-webapp/ /opt/umd-fcrepo-webapp/
COPY src/docker/usr/local/tomcat/ /usr/local/tomcat/

VOLUME /var/umd-fcrepo-webapp
# for the store-and-forward broker
VOLUME /var/activemq
