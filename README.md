# java-instrumentor

Modules have their own `README.md`.

Modules:

 * `agent`: a [Java instrumentation agent](https://docs.oracle.com/javase/8/docs/api/java/lang/instrument/package-summary.html)
   to identify interesting methods, and their calls.
 * `simplest-test`: the simplest hello world in the world
 * `flask-rec`: a server to listen for http posts and show the results
 * `investigations`: some data dumps, and discussion of them


## Demo (breakpoint method)

```bash
bin/trace agent/snyk-goof.properties 1337
```

If the script gets confused, you might have to set `JAVA_HOME`, on Linux this might be:

```bash
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64/
```

Note: It doesn't currently send beacons unless an event has happened.


## Demo (agent method)

(outdated)

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
(export MAVEN_OPTS="-javaagent:$(pwd)/agent/build/libs/agent.jar=file:$(pwd)/agent/snyk.properties" && cd ../java-goof && mvn tomcat7:run)
```

(Shell note: the `()` here are required. They prevent environment and `cd` from
leaking back to the script. Please don't take them out.)

This will post to `homebase`. `homebase` will then hang, because it validates data on output,
not input, and the data we're sending it is invalid.
