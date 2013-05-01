# Weblogic CLI

Weblogic CLI provides a command line tool that can be used to :

* List installed applications
* Deploy an application
  * may target a single server or a cluster of server
  * support "production redeployment" when `Weblogic-Application-Version` is provided in META-INF file.
* Undeploy an application

All of this works remotely, using Weblogic T3 connection, and Weblogic JMX API.

## Installing

Weblogic CLI requires "wlfullclient" as a maven dependency. If you don't have it yet, you can build it by yourself :

```bash
$ cd <WEBLOGIC_INSTALL_DIR>/wlserver/server/lib
$ java -jar wljarbuilder.jar
```

Then install wlfullclient.jar in your local maven repository :

```bash
mvn install:install-file  -Dfile=wlfullclient.jar \
                          -DgroupId=com.oracle.weblogic \
                          -DartifactId=wlfullclient \
                          -Dversion=10.3.5 \
                          -Dpackaging=jar
```

Then, build the project :

```bash
$ mvn install
```

This produces a distribution archive in `target/weblogic-cli-yyyyMMdd-hhmm.tar.gz`.

## Running

First, untar the distribution archive :

```bash
tar zxvf weblogic-cli/target/weblogic-cli-yyyyMMdd-hhmm.tar.gz
```

Edit `environnements.conf` file. Add configuration to connect to your Weblogic admin servers.

You can then run the `weblogic` executable. Help will automatically be displayed if params are incorrect.
