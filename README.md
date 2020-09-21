# Rheem Extension

`rheem-extensions` is the repository, here you can found the implementation of extensions that can be use as plugin to 
rheem

## Enviroment definition

With scala 2.11
```bash
SCALA_VERSION=scala-11
```
With scala 2.12
```bash
SCALA_VERSION=scala-12
```

### How to compile
```bash
mvn clean compile -P scala,${SCALA_VERSION}
```

### How to create the package

```bash
mvn clean package -P scala,${SCALA_VERSION}
```

### How to deploy

```bash
mvn clean deploy -P central,scala,${SCALA_VERSION}
```