We need to be on the Java command-line. This should be easy. Unfortunately, users.

#### in general


Reminder: Java applications are started by the Java command, like this:

`java [jvm options and classpath] application-class-or--jar [application arguments]`

 * e.g. `java -jar foo.jar`
 * e.g. `java -cp '.:../foo/libs/*' org.example.Foo --help`

We need to go before the `application-class-or--jar`.

 * e.g. `java -javaagent:agent.jar=foo.props -jar foo.jar`
 * e.g. `java -cp '.:../foo/libs/*' -javaagent:agent.jar=foo.props org.example.Foo --help`
 * e.g. `java -javaagent:agent.jar=foo.props -cp '.:../foo/libs/*' org.example.Foo --help`

For most tools, there's probably some way to pass flags to the JVM (which is not
the same as the application), this is used for adjusting the memory limit, and
other performance settings. We need to go in there. Look for how `-Xmx`, or
`-XX:MaxPermSize`. Also, some types of `-D` flags are passed before the application,
like `-Djavax.net.ssl.trustStore=`.

If you can specify a path to Java, and the shell script is sloppy enough, that could work!
`JAVA_HOME` is not sufficient.

#### maven-launched things

`MAVEN_OPTS` apply to the actual JVM.

#### glassfish

Workaround: extract what `asadmin` is actually doing

```bash
java -javaagent:agent.jar=... -jar glassfish/lib/client/appserver-cli.jar
```

.. but this only runs it for the CLI, not the server. For that, the UI provides:

![Add JVM Option](https://quad.pe/e/XH6nvmzM0W.png)
