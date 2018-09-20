## Competitors

How do other people do this?


### NewRelic

> The agent is packaged as a JSR 163 compliant javaagent that is activated by the JVM through
 modifications to the JVM launch. Once activated, the agent inserts itself into the class
  loading stream and instruments class methods using byte code instrumentation (bci).
>
> Designed to have minimal impact on your web application, all of the classes are in the
 newrelic package namespace so they do not collide with your own classes. The agent uses
  the ASM bci engine to insert software probes.
>
> The agent receives basic information about your host environment, such as operating system,
 Java version, system properties, and your New Relic configuration file. The agent also polls
  data from the JVM and from JMX.


This is almost precisely what the "javaagent" is doing.

Sounds like they're repackaging ASM into the `newrelic` package, which we should do.


### Elastic(Search) APM

https://github.com/elastic/apm-agent-java

"javaagent" again.


### google-cloud-debug, plumbr

These use a C++ JVMTI agent.

e.g. https://plumbr.io/blog/java/migrating-from-javaagent-to-jvmti-our-experience

Plumbr claim to be using the MethodEntry system, which I know to be terrible for
performance. The API looks very similar to the debug api, though, unsurprisingly.


### Opsian

Pretty sure this is an application masquerading as an JVMTI agent, which uses
a novel snapshotting method.
