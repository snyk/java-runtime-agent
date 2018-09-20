# Risks

What can go wrong?

### Categories

In rough order of horror:

 * Application produces the wrong answers
 * Cause a security vulnerability in the Application
 * Crash, and take the Application with us
 * Prevent Application from starting
 * Some "significant" slowdown to the execution of the application
 * User can't work out how to install agent
 * Agent DoSes snyk.io and we have to ring up the customer

### Considered mitigations

What have we already thought of?

 * Work only on whitelisted methods
 * Aggressive error handling (i.e. log and continue)

---

And, on to the issues themselves:

### Incorrect computation

`javaagent` is making a very small change to the code of specified methods.
These methods will be in public libraries, which we will have seen and can analyse.

Bad cases:

 * We cause an error, and they catch it and silently change behaviour
 * Performance change causes threaded code to produce different results
     due to ordering, and people think this is our fault.

Under *whitelisting*, if a method is plausible causing issues it can be removed.
We can not add any method which "looks suspicious"; pick another, nearby, more
normal method.


### Cause security vulnerabilities

We're in Java, so we'd have to mess up pretty badly. Only "input" is from any
web requset we do back to snyk, and handle the result, e.g. to request a rules file.

Any other issue would be similar to "incorrect computation", see above.


### Crash, and take the application with us

Most of the work we do is during startup, or on an independent thread, in Java.

At startup, this will be effectively the same as preventing the app from starting.
My experience is that people are way happier with errors that happen at startup,
instead of happening at 3am while they're on call.

Perhaps we should even *disable* instrumentation of things loaded more than a few
minutes after startup, just to catch this.

Work on the independent thread is pretty independent, most types of errors wouldn't
cause an issue or a deadlock. We could exhaust memory. The way this works in common
JVMs is very messy. We have to allocate. We need to make sure that it's reasonable,
and we don't horribly die if we're the victim when the memory reaper comes along.


### Break startup

We are doing work at startup. We can ignore *some* problems that occur during the
work. There's plenty of validation for the work we've done, but a validation failure
might not be reported to us, it might be reported by the JVM going down, possibly
quite hard.

Under *whitelisting*, we are only doing hard work on:

 * determining the source of classes, for the System Information component
    (which we could turn off)
 * rewriting whitelisted classes, which we should have seen before

This is relatively low risk.

The NewRelic breakage was at startup: we rewrote their agent, and it could no longer
operate. They gracefully handled this error (by complaining loudly), but many wouldn't.

The NewRelic agent would not be on the *whitelist*.


### Ruin performance

We add some larger overhead at startup (something like 30% slower loading),
and some ("constant") overhead when a method is called (nanoseconds).

Both of these are typically irrelevant, but there are times someone might notice.

When monitoring all methods (i.e. not *whitelisted*), it's possible we'll pick to
monitor a very hot/cheap method, and our "constant" overhead will be very large,
when compared to the hot/cheap method, and hence there will be a significant
slowdown.

That is, whitelisted methods need to be picked to not be super cheap/hot.

Chris is pretty confident on the performance. These methods have to be *super* cheap/hot.
e.g. `class SomeArray { Element get(int idx) { return this.store[idx]; }` is not okay,
but `long nums(String a, String b) { return parseLong(a) + parseLong(b); }` is.

The other case this can go wrong is where they have a very short-lived process,
for which they notice the startup overhead. e.g. a job takes 16s normally, of which 12s is
startup, and we add 30% to that 12s, giving (arguably) a 100% slowdown. Most people don't
write Java applications like this, though.


### Installation problems

If the agent isn't installed right, there's very little we can do. Supporting this will
be hard. People like NewRelic have lots of documentation on ways to do it, plus a script
that does it automatically in many situations. I would expect many developers to have an
issue adding a command line option to their production app, because their production app
will normally be started through a system they don't understand or care about.

You have to get the path to the file right, or you get (practically?) no error, the agent
will just not load.

If we load, and we can't find the config, or can't reach home, we can print errors to log
files, but those log files might not be readable to users; e.g. stderr might be eaten/lost,
and they might not have access to the machine to recover `/tmp/snyk-agent.9678cabe/logs`.


### Self DoS

The agent is pushing data. It needs to limit the amount of data it's pushing (both frequency
and volume) and/or honour
[kiss of death](https://en.wikipedia.org/wiki/NTP_server_misuse_and_abuse#Technical_solutions).

Other suggestions:

 * different DNS endpoint per client, for black-holing or scaling
