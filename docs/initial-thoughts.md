# agent

## What?

We want to monitor whether an "interesting"
"method" has been called "recently", with a "low overhead".

This agent attempts to do this using the `javaagent` framework,
which can rewrite classes on "load" (effectively, at startup).

This is problematic as, if a new vulnerabile method is found,
the rule "can't" be patched into the running application.
It's expected that users won't restart their applications often
enough to be protected by only on-start instrumentation.


## How?

 * on load, examine a method, and decide if it's "interesting"
 * if it's "interesting", insert some instrumentation to
    capture that's it's been called, with minimal overhead
 * "every so often", tell the server what's been run
 * if the server has extra requirements, maybe honour its requests


## "interesting"

"interesting" means that a method could be part of an exploit
that we might want to track.

A couple of categories come to mind:

 * public methods of popular libraries, which could be utility
    methods which could have an issue
 * a known blacklist, e.g. `new ProcessBuilder()`
 * Any method taking non-primitive types (i.e. an array,
   a `String`, an `Object`)?

Or, we could exclude:

 * getters/setters
 * Any method taking only primitive types, etc.


## "method"

Since Java 8 / Java 9, rewriting core classes seems to be
more problematic. This needs some research.

You could argue that vulnerabilities in core classes aren't
our problem, but it would be interesting to track e.g. those
`ProcessBuilder` calls.


## "recently"

The plan in `node` land is to track whether a function has
been called inside a configurable window, and to tell the
server about that. Think 1h, 3h. Chris worries that this list
will be very long.


## "low overhead"

In Java we have two things to consider here:

 * how well the compiler will optimise away any code we insert
 * how much work we can do, and how hot the method is

Waking up in the background to gather things is probably not
important.

We almost certainly want to avoid allocation on the hot path,
as that makes users angry.

The current implementation breaks every single one of these
rules.


## Status

Pretty much everything here is demo-grade garbage. Watch out for
code with `TODO` comments, and even more for code without comments.
