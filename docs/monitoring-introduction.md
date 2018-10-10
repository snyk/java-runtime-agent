## Monitoring introduction

We want to know whether some methods have been called.

This can be done with "rewriting", a "[debugger](debugger.md)", 
"internal hooking", or "profiling".

We're doing "rewriting", as our most [competitors](competitors.md).

There are a few "internal hooking" products. These have a much higher
engineering cost and risk. In `node` land, the "debugger" approach seems to
be the way to go. "profiling" is more appropriate for performance monitoring
(APM), instead of security analysis. There's some further justifications in
the [initial thoughts](initial-thoughts.md).


### Rewriting

We want to know when a method is called. We do this by changing the
method's code, from:

```java
void aMethod() {
    stuff();
    otherStuff();
}
```

... into this:

```java
void aMethod() {
    Instrumentation.someMethodWasCalled("aMethod at line..");
    stuff();
    otherStuff();
}
```

.. and provide a useful implementation of `Instrumentation.someMethodWasCalled`.
That's pretty much it!


### Adding the call

The JVM has a system that allows us to edit the bytes of a `.class` file
before it is loaded. We use [ASM](https://asm.ow2.io/) to manipulate the
`.class` file using a higher level API. This higher level API is not Java
code, but Java Bytecode; an assembly-like language.

In bytecode (a language you practically never see in textual form, so
probably unfamiliar!), the stub we add looks like this:

```
ldc our_method_id_here
invokestatic Instrumentation.someMethodWasCalled(I)V
```

 * `ldc`: "LoaD Constant": push the id onto the stack, as the first
    argument of the method call
 * `invokestatic`: "invoke static method": call the instrumentation
    method.

That's almost literally it. (code: `Rewriter#addInspectionOfMethodEntry`).
The rest of the code is about the API, and dealing with odd cases:

 * methods which can't be instrumented (interface methods, native methods, ...)
 * methods we don't want to instrument (empty methods?, blacklisted, ...)
 * etc.

We additionally do extra work here to search for metadata about the
class being loaded, see `ClassInfo#findSourceInfo`.

### Instrumentation implementation

The instrumentation task is split into two parts: gathering the data,
and sending snapshots off to `homebase` (our cloud data service).

Gathering the data is conceptually `wasCalled[our_method_id] = true`.

Sending the snapshot is conceptually `http.post(homebase, wasCalled)`.

The rest is just easy problems, like "performance" and "thread-safety".

The code starts at `LandingZone#registerMethodEntry`, which is the
actual call we insert. This records calls in our `SeenSet`. These calls
are occasionally `drain()ed` by our `ReportingWorker`, and posted off.
