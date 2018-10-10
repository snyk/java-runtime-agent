## struts

Investigating the struts vulnerability exploit demonstrated in the
`java-goof` project.

 * `before.txt`: a normal GET request
 * `after.txt`: an exploit GET request
 * `diff.diff`: the difference in function sets between these two files.
 * `after-loader-names.txt`: Watch for (certain) `loadClass()` calls,
     and capture their arguments.


## Suspicious?

The OgnlRuntime being triggered seems suspicious, but it's not the exploit:

 * `ognl/OgnlRuntime:classForName:classForName:(Ljava/lang/String;Ljava/util/Map;)Ljava/lang/Class;:185`
 * `ognl/OgnlRuntime:callStaticMethod:classForName:(Lognl/OgnlContext;Ljava/lang/String;)Ljava/lang/Class;:184`

Would need to inspect the arguments?

## Arguments

The dynamic loading of `ProcessBuilder` can be observed in
`after-loader-names.txt`, but there's issues with this approach:

 * It's definitely not wrong that classes are being loaded from this classloader.
 * Lots of other classes are loaded at the same time;
    perhaps because the engine is cold.
 * `ProcessBuilder` is not necessarily a bad class.
 * `ProcessBuilder` is purely in the exploit payload, not an actual issue in
    the codebase.
