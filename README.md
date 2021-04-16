# UMD Libraries Fedora Web Application

This is a custom build of Fedora as a servlet-deployable web application.

## Building

```bash
mvn clean package
```

The resulting `umd-fcrepo-webapp-{version}.war` file will be in the `target` directory.

## Running with Cargo

The application can also be run locally using the [Maven Cargo plugin] and
`cargo:run` command. This requires the WAR file to already be built using
`mvn package`.

The [POM file](pom.xml) contains configuration suitable for running the
application using the [umd-fcrepo-docker] stack to provide the backing
services. However, this does require you to remove the `umd-fcrepo_repository`
service after deploying the stack:

```bash
# in the umd-fcrepo-docker directory
docker stack deploy -c umd-fcrepo.yml umd-fcrepo
docker service rm umd-fcrepo_repository
``` 

You must also provide environment variables for the LDAP bind password, Postgres
database password, and JWT secret:

```bash
mvn clean package

export MODESHAPE_DB_PASSWORD=...  # default in the umd-fcrepo-docker stack is "fcrepo"
export LDAP_BIND_PASSWORD=...     # see the SSDR "Identities" document for this
export JWT_SECRET=...             # can be anything, but must be sufficiently long
                                  # one method to generate a random secret is:
                                  #   uuidgen | shasum -a256 | cut -d' ' -f1
mvn cargo:run
```

* <http://localhost:8080/> - Main splash page
* <http://localhost:8080/rest> - REST API endpoint
* <http://localhost:8080/user> - Login/user profile page

### Configuration

The [IpMapperFilter], which determines access rights to resources, uses the 
[src/test/resources/test-ip-mapping.properties] file by default, which does not
provide any access rights for the `localhost` user. This can be modified by
adding the localhost address (`127.0.0.1/32`) to a category in the
`test-ip-mapping.properties` file, or by specifying a different file in the
[pom.xml](pom.xml) file.

The [BasicAuthFilter], which processes `Authorization: Basic ...` HTTP headers
(if present), is configured by [src/test/resources/basic-auth.properties]. The
following users are configured:

| Username | Password | Role      |
|----------|----------|-----------|
|loris     |loris     |fedoraAdmin|

### Environment Variables

These values MUST be set, either via environment variables of Java system
properties, to run the application:

| Name | Provided by `cargo:run` | Value provided by `cargo:run` |
|:--------------------------|:---|:---------------|
|`ACTIVEMQ_URL`                |✓|tcp://localhost:61616|
|`CAS_URL_PREFIX`              |✓|https://shib.idm.umd.edu/shibboleth-idp/profile/cas|
|`FCREPO_BASE_URL`             |✓|http://localhost:8080/|
|`IP_MAPPING_FILE`             |✓|conf/test-ip-mapping.properties|
|`CREDENTIALS_FILE`            |✓|conf/basic-auth.properties|
|`JWT_SECRET`                  | ||
|`LDAP_URL`                    |✓|ldap://directory.umd.edu|
|`LDAP_BASE_DN`                |✓|ou=people,dc=umd,dc=edu|
|`LDAP_BIND_DN`                |✓|uid=libr-fedora,cn=auth,ou=ldap,dc=umd,dc=edu|
|`LDAP_BIND_PASSWORD`          | ||
|`LDAP_MEMBER_ATTRIBUTE`       |✓|memberOf|
|`LDAP_ADMIN_GROUP`            |✓|cn=Application_Roles:Libraries:FCREPO:FCREPO-Administrator,ou=grouper,ou=group,dc=umd,dc=edu|
|`LDAP_USER_GROUP`             |✓|cn=Application_Roles:Libraries:FCREPO:FCREPO-User,ou=grouper,ou=group,dc=umd,dc=edu|
|`MODESHAPE_DB_DRIVER`         |✓|org.postgresql.Driver|
|`MODESHAPE_DB_URL`            |✓|jdbc:postgresql://localhost:5432/fcrepo_modeshape5|
|`MODESHAPE_DB_USERNAME`       |✓|fcrepo|
|`MODESHAPE_DB_PASSWORD`       | ||

## Docker

This repository contains a [Dockerfile](Dockerfile) for creating the image to
use with the [umd-fcrepo-docker] stack:

```bash
docker build -t docker.lib.umd.edu/fcrepo-webapp .
```

## Special Thanks

This repository is based on the [Amherst College custom Fedora build](https://gitlab.amherst.edu/acdc/amherst-fedora-webapp) created and maintained by Aaron Coburn and Bethany Seeger.


## License

See the [LICENSE](LICENSE.md) file for license rights and limitations (Apache 2.0).

[Maven Cargo plugin]: https://codehaus-cargo.github.io/cargo/Maven2+plugin.html
[umd-fcrepo-docker]: https://github.com/umd-lib/umd-fcrepo-docker
[IpMapperFilter]: src/main/java/edu/umd/lib/fcrepo/IpMapperFilter.java
[src/test/resources/test-ip-mapping.properties]: src/test/resources/test-ip-mapping.properties
[BasicAuthFilter]: src/main/java/edu/umd/lib/fcrepo/BasicAuthFilter.java
[src/test/resources/basic-auth.properties]: src/test/resources/basic-auth.properties
