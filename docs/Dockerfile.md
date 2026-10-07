# umd-fcrepo-webapp Dockerfile

* Base image: tomcat:10.1.57-jdk25-temurin
* Ports: 8080

## Important Filesystem Paths

| Path                     | Volume? | Purpose                                       |
|--------------------------|:-------:|-----------------------------------------------|
| `/opt/umd-fcrepo-webapp` |         | Webapp deployment directory                   |
| `/usr/local/tomcat`      |         | Tomcat root (`CATALINA_BASE`/`CATALINA_HOME`) |
| `/usr/local/tomcat/conf` |         | Tomcat configuration                          |
| `/var/umd-fcrepo-webapp` |    ✓    | Repository content                            |
| `/var/activemq`          |    ✓    | Message broker persistent queues              |

## Custom Configuration

### Webapp

These files are included by the Maven build.

* Source directory: [src/main/resources](../src/main/resources)
* Deployed directory in the Docker image: `/opt/umd-fcrepo-webapp/WEB-INF/classes`

| Name                       | Purpose                                                |
|----------------------------|--------------------------------------------------------|
| `spring/fcrepo-config.xml` | Spring beans for servlet configuration (mostly authNZ) |
| `default-acl.ttl`          | Default deny "backstop" ACL for Fedora                 |
| `logback.xml`              | Log4j2 loggers configuration                           |
| `namespaces.yml`           | RDF namespace prefix definitions                       |

### Docker Image

These files are included by the Docker build.

* Source directory: [src/docker/usr/local/tomcat/conf](../src/docker/usr/tomcat/conf)
* Deployed directory in the Docker image: `/usr/local/tomcat/conf`

| Name                  | Purpose                             |
|-----------------------|-------------------------------------|
| `catalina.properties` | Low-level Tomcat Java configuration |
| `logging.properties`  | Log4j2 log format configuration     |
| `server.xml`          | Tomcat server configuration         |


### Docker Compose

These files are included by the Docker compose file.

* Source directory: [conf](../conf)
* Deployed directory in the Docker image: `/usr/local/tomcat/bin`

| Name        | Purpose                                         |
|-------------|-------------------------------------------------|
| `setenv.sh` | Runtime system properties for Tomcat and Fedora |
