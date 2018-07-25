# java-instrumentor

Modules have their own `README.md`.

Modules:

 * `agent`: a [Java instrumentation agent](https://docs.oracle.com/javase/8/docs/api/java/lang/instrument/package-summary.html)
   to identify interesting methods, and their calls.
 * `simplest-test`: the simplest hello world in the world
 * `flask-rec`: a server to listen for http posts and show the results
 * `investigations`: some data dumps, and discussion of them


## Demo

The `agent` wil run against the snyk standard `java-goof` project. The instructions
here assume they're checked out like this, and you're inside this directory, but
none of the layout is required:

```
faux@errata:~/code% ls -1
java-goof
java-instrumentor
```

In a terminal, run:

```
(cd agent && ./gradlew shadow)
(export MAVEN_OPTS="-javaagent:$(pwd)/agent/build/libs/agent.jar" && cd ../java-goof && mvn tomcat7:run)
```

In another terminal, run:

 1. `(cd flask-rec && flask run)`

(You'll need to follow the setup instructions in `flask-rec` if you haven't already,
unless you are using the only machine in the world with a working `flask` install.)

(Shell note: the `()` here are required. They prevent environment and `cd` from
leaking back to the script. Please don't take them out.)

Open these links in your browser:

 1. http://localhost:8080
 2. http://localhost:5000/report

Port numbers are hardcoded, sorry.
