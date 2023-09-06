# Convert any environment variables that start with FCREPO_ into
# property declarations. Replaces "_" with ".", and transforms the
# key into all lowercase.
#
# Examples::
#
#   # input environment
#   FCREPO_HOME=/var/umd-fcrepo-webapp
#   FCREPO_LOG_AUTH=DEBUG
#
#   # output line
#    -Dfcrepo.home=/var/umd-fcrepo-webapp -Dfcrepo.log.auth=DEBUG
#
function get_env_opts {
    env \
    | grep -e '^FCREPO_' \
    | while IFS='=' read -r key value; do printf " -D$(tr 'A-Z' 'a-z' <<<"$key" | tr '_' '.')=$value"; done
}

export CATALINA_OPTS="-XX:+UseConcMarkSweepGC \
  -XX:+CMSClassUnloadingEnabled \
  -XX:ConcGCThreads=5 \
  -XX:MaxGCPauseMillis=200 \
  -XX:ParallelGCThreads=20 \
  -XX:MaxMetaspaceSize=512M \
  -Xms${TOMCAT_HEAP} \
  -Xmx${TOMCAT_HEAP} \
  -Dfile.encoding=UTF-8 \
  -Dfcrepo.home=/var/umd-fcrepo-webapp \
  -Dfcrepo.activemq.directory=/var/activemq \
  -Dfcrepo.context.path=${CONTEXT_PATH} \
  -Dconnection.timeout=${CONNECTION_TIMEOUT} \
  $(get_env_opts)"
