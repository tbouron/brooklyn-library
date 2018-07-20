
# [![**Brooklyn**](https://brooklyn.apache.org/style/img/apache-brooklyn-logo-244px-wide.png)](http://brooklyn.apache.org/)

### Library of Entities for Apache Brooklyn

This sub-project contains various entities not *needed* for Brooklyn,
but useful in practice as building blocks, including entities for webapps,
datastores, and more.

### Building the project

2 methods are available to build this project: within a docker container or directly with maven.

#### Using docker

The project comes with a `Dockerfile` that contains everything you need to build this project.
First, build the docker image:

```bash
docker build -t brooklyn:library .
```

Then run the build:

```bash
docker run -i --rm --name brooklyn-library -v ${HOME}/.m2:/root/.m2 -v ${PWD}:/usr/build -w /usr/build brooklyn:library mvn clean install
```

### Using maven

Simply run:

```bash
mvn clean install
```