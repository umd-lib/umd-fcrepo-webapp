# Dockerfile for the generating the webapp image
#
# To build:
#
# docker build -t docker.lib.umd.edu/fcrepo-webapp:<VERSION> -f Dockerfile .
#
# where <VERSION> is the Docker image version to create.
FROM maven:3.6.3-jdk-8-slim AS compile

ENV SOURCE_DIR /opt/umd-fcrepo-webapp
COPY src $SOURCE_DIR/src
COPY pom.xml $SOURCE_DIR
WORKDIR $SOURCE_DIR
RUN mvn package -DwarFileName=umd-fcrepo-webapp

FROM tomcat:7.0.106-jdk8-openjdk-buster

COPY --from=compile /opt/umd-fcrepo-webapp/target/umd-fcrepo-webapp.war /usr/local/tomcat/webapps/ROOT.war
COPY setenv.sh /usr/local/tomcat/bin/

VOLUME /var/umd-fcrepo-webapp
