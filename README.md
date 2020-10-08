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
the [umd-fcrepo-docker](https://github.com/umd-lib/umd-fcrepo-docker) stack to provide the
backing services.

You must also provide environment variables for the LDAP bind password and the Postgres
database password:

```bash
mvn clean package
export FCREPO_DB_PASSWORD=...      # default in the umd-fcrepo-docker stack is "fcrepo"
export UMD_LDAP_BIND_PASSWORD=...  # see the SSDR "Identities" document for this
mvn cargo:run
```

* <http://localhost:8080/> - Main splash page
* <http://localhost:8080/rest> - REST API endpoint
* <http://localhost:8080/user> - Login/user profile page

## Special Thanks

This repository is based on the [Amherst College custom Fedora build](https://gitlab.amherst.edu/acdc/amherst-fedora-webapp) created and maintained by Aaron Coburn and Bethany Seeger.


## License

See the [LICENSE](LICENSE.md) file for license rights and limitations (Apache 2.0).

