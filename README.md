# UMD Libraries Fedora 4 Web Application

This is a custom build of Fedora as a servlet-deployable web application.

## Configuration

The following configuration properties are available:


### Spring configuration

```
fcrepo.spring.configuration=classpath:spring/configuration.xml
```

The Spring configuration is consolidated into a single file to make editing easier. The entire configuration can be externalized in order to manage that configuration separately, in which case, the value could be `file:/apps/fedora/config/spring.xml`.

### Modeshape configuration

```
fcrepo.modeshape.configuration=file:/apps/fedora/config/repository.json
```

The modeshape configuration should be managed separately from the running servlet container. This value makes it possible.

### ActiveMQ Configuration

```
fcrepo.activemq.configuration=file:/apps/fedora/config/activemq.xml
```

Likewise, the ActiveMQ configuration should be managed separately from the running servlet container.


## Building

The web application can be built with Maven:

```
$ mvn clean package
```

The resulting `umd-fcrepo-webapp-{version}.war` file will be in the `target` directory.


## Special Thanks

This repository is based on the [Amherst College custom Fedora build](https://github.com/acoburn/amherst-fedora-webapp) created and maintained by Aaron Coburn and Bethany Seeger.

