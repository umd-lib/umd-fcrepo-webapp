#!/bin/sh

# The EnvironmentPropertySource adds system properties for env variables defined in docker image

CATALINA_OPTS="$CATALINA_OPTS \
  -Dfcrepo.home=/var/umd-fcrepo-webapp \
  -Dfcrepo.jms.destination.type=queue \
  -Dfcrepo.activemq.directory=/var/activemq/kahadb \
  -Dfile.encoding=UTF-8 \
  -Dorg.apache.tomcat.util.digester.PROPERTY_SOURCE=org.apache.tomcat.util.digester.EnvironmentPropertySource \
  -Dorg.apache.tomcat.util.digester.REPLACE_SYSTEM_PROPERTIES=true"
