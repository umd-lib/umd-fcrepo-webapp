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

# Note: Pinning the tomcat image to this precise image hash, because it uses
# OpenJDK v8u265. A later version of this image uses OpenJDK v8u272, which
# has an LDAP issue similar to https://bugs.openjdk.java.net/browse/JDK-8214440
# This sha256 was recovered from the UMD Nexus by examining the "manifests"
# folder for the "tomcat" image and examining the "last_modified" dates of
# when they were downloaded.
#
# See LIBFCREPO-903 for more information and a tester program.
FROM tomcat:7.0.106-jdk8-openjdk-buster@sha256:7389e901db3b2f9bb0268ce4cbd2ec2e1010db1ef43e04c49a64d96b156d0022

# default context path is the root, making the full URL e.g. http://localhost:8080/
ENV CONTEXT_PATH=""
RUN mkdir -p /opt/umd-fcrepo-webapp
COPY --from=compile /opt/umd-fcrepo-webapp/target/umd-fcrepo-webapp.war /opt/umd-fcrepo-webapp/
COPY setenv.sh /usr/local/tomcat/bin/
COPY server.xml /usr/local/tomcat/conf/

VOLUME /var/umd-fcrepo-webapp
