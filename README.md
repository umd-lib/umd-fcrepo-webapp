# Amherst College Fedora

This is a custom build of Fedora as a servlet-deployable web application.
It includes WebAC authentication, JMS messaging and not much else.

The REST endpoint is available at `http://localhost:8080/fcrepo/linkeddata`.

## Configuration

The following configuration properties are available:


### Spring configuration

    fcrepo.spring.configuration=classpath:spring/configuration.xml

The Spring configuration is consolidated into a single file to make editing easier.
The entire configuration can be externalized in order to manage that configuration separately,
in which case, the value could be `file:/etc/fcrepo/spring.xml`.

### Modeshape configuration

    fcrepo.modeshape.configuration=file:/etc/fcrepo/repository.json

The modeshape configuration should be managed separately from the running servlet container. This
value makes it possible.

### ActiveMQ Configuration

    fcrepo.activemq.configuration=file:/etc/fcrepo/activemq.xml

Likewise, the ActiveMQ configuration should be managed separately from the running servlet container.


## Building

The web application can be built with gradle:

    ./gradlew build

The compiled application can be found in `./build/libs/amherst-fedora-webapp.war`.


## Maintainers

  * Aaron Coburn
  * Bethany Seeger

