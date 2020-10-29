# UMD Libraries Fedora 4 Web Application

This is a custom build of Fedora as a servlet-deployable web application.

## Building

```bash
mvn clean package
```

The resulting `umd-fcrepo-webapp-{version}.war` file will be in the `target` directory.

## Running with Cargo

The application can also be run locally using the Maven Cargo plugin and `cargo:run` command.
This requires the WAR file to be built using `mvn package` already.

The [POM file](pom.xml) contains configuration suitable for running the application using
the [umd-fcrepo-docker] stack to provide the backing services.

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

The IpMapperFilter, which determines access rights to resources, uses the 
[src/test/resources/test-ip-mapping.properties](src/test/resources/test-ip-mapping.properties)
file by default, which does not provide any access rights for the "localhost" user. This can
be modified by adding the localhost address (`127.0.0.1/32`) to a category in the 
"test-ip-mapping.properties" file, or by specifying a different file in the [pom.xml](pom.xml) file. 

## Docker

This repository contains a Dockerfile for creating the image to use with the [umd-fcrepo-docker] stack:

```bash
docker build -t docker.lib.umd.edu/fcrepo-webapp .
```

## Configuration

These values MUST be set, either via environment variables of Java system properties, to run
the application:

| Name | Provided by `cargo:run` | Standard Value |
|:--------------------------|:---|:---------------|
|`ACTIVEMQ_URL`                |✓||
|`CAS_URL_PREFIX`              |✓|https://shib.idm.umd.edu/shibboleth-idp/profile/cas|
|`FCREPO_BASE_URL`             |✓||
|`IP_MAPPING_FILE`             |✓||
|`JWT_SECRET`                  | ||
|`LDAP_URL`                    |✓|ldap://directory.umd.edu|
|`LDAP_BASE_DN`                |✓|ou=people,dc=umd,dc=edu|
|`LDAP_BIND_DN`                |✓|uid=libr-fedora,cn=auth,ou=ldap,dc=umd,dc=edu|
|`LDAP_BIND_PASSWORD`          | ||
|`LDAP_MEMBER_ATTRIBUTE`       |✓|memberOf|
|`LDAP_ADMIN_GROUP`            |✓|cn=Application_Roles:Libraries:FCREPO:FCREPO-Administrator,ou=grouper,ou=group,dc=umd,dc=edu|
|`LDAP_USER_GROUP`             |✓|cn=Application_Roles:Libraries:FCREPO:FCREPO-User,ou=grouper,ou=group,dc=umd,dc=edu|
|`MODESHAPE_DB_DRIVER`         |✓|org.postgresql.Driver|
|`MODESHAPE_DB_URL`            |✓||
|`MODESHAPE_DB_USERNAME`       |✓||
|`MODESHAPE_DB_PASSWORD`       | ||

## Special Thanks

This repository is based on the [Amherst College custom Fedora build](https://gitlab.amherst.edu/acdc/amherst-fedora-webapp) created and maintained by Aaron Coburn and Bethany Seeger.


## License

See the [LICENSE](LICENSE.md) file for license rights and limitations (Apache 2.0).

[umd-fcrepo-docker]: https://github.com/umd-lib/umd-fcrepo-docker
