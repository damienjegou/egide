egide
=====

Egide is an autonomous trading bot component
Egide connects to lmax, collects data, and publish it through redis pubsub
messaging system.

Add LMAX java lib to project (maven2 required)
----------------------------------------------

```bash
$ mkdir maven_repo
```

Add java-api.jar to local repo :
```bash
$ mvn install:install-file -Dfile=java-api.jar -DartifactId=java-api -Dversion=1.8.1.0 -DgroupId=java-api -Dpackaging=jar -DlocalRepositoryPath=maven_repo
$ lein deps
```

