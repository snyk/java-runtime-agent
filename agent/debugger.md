## Debugger

There's an official way to do this, and a good way.

The official way is to ask the JVM to monitor `MethodEntry` for a class.
For an unknown reason, this is pretty slow (as most Java developers would be able
to tell you, from having used an IDE). My tests put it at a >5000x slowdown, which is
way worse than my expectation, of ~3x.

The good way is to set a normal breakpoint "near the start" of "valid" methods.

This works well, but isn't guaranteed to capture pathological cases, and might go
wrong. We probably don't care. We likely won't ever need to filter on a pathological
case, and, if anything goes wrong, at worst, we lose the monitoring, we don't ruin the
target (afaik).

### Advantages

 * Can dynamically add and remove breakpoints, during monitoring
 * Client/agent is a much more normal Java application,
    which can load libraries, and be debugged,
 * We can crash without messing anything up (afaik).
 * Dynamic disabling restores original speed (afaik).
 * Can disconnect and reconnect at runtime.
 
### Disadvantages
 
 * Much, *much* more complicated application startup, except with launcher?
 * Some cases where we could miss something,
    but probably fewer than the instrumentation case.
 * MethodEntry performance is worse than expected. Way worse.
 * Agent can cause deadlocks, which will hang the target forever.


With debugger, 24, ab:

```
Percentage of the requests served within a certain time (ms)
  50%     33
  66%     41
  75%     46
  80%     50
  90%     60
  95%     69
  98%     80
  99%     87
 100%    159 (longest request)
```

Without debugger, 24, ab:
```
  50%     60
  66%     72
  75%     80
  80%     86
  90%    101
  95%    113
  98%    126
  99%    135
 100%    179 (longest request)
```

Oh good. It's faster with the instrumentation.

Oh good.
