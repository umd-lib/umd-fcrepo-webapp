# umd-fcrepo-webapp

This is a custom build of [Fedora] as a servlet-deployable web
application, including additional authentication and authorization
configuration for UMD Libraries.

## Quick Start

Clone the repo:

```zsh
git clone git@github.com:umd-lib/umd-fcrepo-webapp.git
cd umd-fcrepo-webapp
```

Create a `.env` file with the following variables:

| Name                 | Value                                       |
|----------------------|---------------------------------------------|
| `JWT_SECRET`         | any sufficiently long random string         |
| `LDAP_BIND_PASSWORD` | available in the SSDR LastPass shared items |

The output of `uuidgen | base64` makes a good `JWT_SECRET` value.

Build and run the Docker image using Docker Compose:

```zsh
docker compose build
docker compose up
```

To ensure that CAS authentication works correctly, you will need to add
the hostname `fcrepo-local` to your `/etc/hosts` file:

```
127.0.0.1  fcrepo-local
```

The web application will be running at <http://fcrepo-local:8080/fcrepo>

## Component Versions

* [Java 25.0] (Eclipse Temurin)
* [Tomcat 10.1]
* [Fedora 7.0]

## Docker

To build the Docker image:

```zsh
docker build -t docker.lib.umd.edu/fcrepo-webapp:latest .
```

The configuration files used to create the Docker image are located in the
[src/docker](src/docker) directory, in a path structure that mirrors their
destination locations in the image.

Additional runtime configuration files used by the Docker Compose stack
are located in the [conf](conf) directory.

## Environment Variables

| Name                     | Provided by `compose.yml` | Value provided by `compose.yml`                                                              |
|:-------------------------|:--------------------------|:---------------------------------------------------------------------------------------------|
| `CAS_URL_PREFIX`         | ✓                         | https://shib.idm.umd.edu/shibboleth-idp/profile/cas                                          |
| `CONTEXT_PATH`           | ✓                         | /fcrepo                                                                                      |
| `FCREPO_BASE_URL`        | ✓                         | http://fcrepo-local:8080/                                                                    |
| `FCREPO_LOG_LEVEL`       | ✓                         | DEBUG                                                                                        |
| `IP_MAPPING_HEADER_NAME` | ✓                         | X-Auth-IP-Mapping                                                                            |
| `JWT_SECRET`             |                           |                                                                                              |
| `LDAP_URL`               | ✓                         | ldap://directory.umd.edu                                                                     |
| `LDAP_BASE_DN`           | ✓                         | ou=people,dc=umd,dc=edu                                                                      |
| `LDAP_BIND_DN`           | ✓                         | uid=libr-fedora,cn=auth,ou=ldap,dc=umd,dc=edu                                                |
| `LDAP_BIND_PASSWORD`     |                           |                                                                                              |
| `LDAP_MEMBER_ATTRIBUTE`  | ✓                         | memberOf                                                                                     |
| `LDAP_ADMIN_GROUP`       | ✓                         | cn=Application_Roles:Libraries:FCREPO:FCREPO-Administrator,ou=grouper,ou=group,dc=umd,dc=edu |
| `LDAP_USER_GROUP`        | ✓                         | cn=Application_Roles:Libraries:FCREPO:FCREPO-User,ou=grouper,ou=group,dc=umd,dc=edu          |
| `UMD_LIB_LOG_LEVEL`      | ✓                         | DEBUG                                                                                        |

## Logging Configuration

This application has a [logback.xml](src/main/resources/logback.xml) that has
been customized from the upstream fcrepo version of this configuration. The
customizations are:

* Changed the names of the system properties used to set the log levels for
  the various loggers to an environment variable style (all-caps and with `_`
  instead of `.` as a separator). The purpose is to make runtime configuration
  of logging easier in Docker and Kubernetes contexts.
* Added a property to control the log level of the `edu.umd` package, allowing
  configuration of logging in our custom servlets, filters, and wrappers.

| Logger                    | Environment Variable            | Default in `logback.xml` |
|---------------------------|---------------------------------|--------------------------|
| Root Logger               | `LOG_LEVEL`                     | WARN                     |
| `org.fcrepo`              | `FCREPO_LOG_LEVEL`              | INFO                     |
| `edu.umd.lib`             | `UMD_LIB_LOG_LEVEL`             | INFO                     |
| `org.fcrepo.auth`         | `FCREPO_AUTH_LOG_LEVEL`         |                          |
| `org.fcrepo.config`       | `FCREPO_CONFIG_LOG_LEVEL`       |                          |
| `org.fcrepo.event`        | `FCREPO_EVENT_LOG_LEVEL`        |                          |
| `org.fcrepo.http.api`     | `FCREPO_HTTP_API_LOG_LEVEL`     |                          |
| `org.fcrepo.http.commons` | `FCREPO_HTTP_COMMONS_LOG_LEVEL` |                          |
| `org.fcrepo.jms`          | `FCREPO_JMS_LOG_LEVEL`          |                          |
| `org.fcrepo.kernel`       | `FCREPO_KERNEL_LOG_LEVEL`       |                          |
| `org.fcrepo.persistence`  | `FCREPO_PERSISTENCE_LOG_LEVEL`  |                          |
| `org.fcrepo.search`       | `FCREPO_SEARCH_LOG_LEVEL`       |                          |
| `org.fcrepo.storage`      | `FCREPO_STORAGE_LOG_LEVEL`      |                          |

## Development

To develop and build the project locally, you will need Java 25.0. There is
a `.java-version` file in the project which will select the correct JDK for
you if you have [jenv] installed.

To build the WAR file:

```bash
mvn clean install
```

The resulting `umd-fcrepo-webapp-{version}.war` file will be in the `target`
directory.

## Special Thanks

This repository is originally based on the
[Amherst College custom Fedora build](https://gitlab.amherst.edu/acdc/amherst-fedora-webapp)
created and maintained by Aaron Coburn and Bethany Seeger.

## License

See the [LICENSE](LICENSE.md) file for license rights and limitations
(Apache 2.0).

[Fedora]: https://fedorarepository.org/
[jenv]: https://www.jenv.be/
[Java 25.0]: https://adoptium.net/temurin/release-notes?version=25
[Tomcat 10.1]: https://tomcat.apache.org/tomcat-10.1-doc/index.html
[Fedora 7.0]: https://fedorarepository.org/fedora7announcement/
