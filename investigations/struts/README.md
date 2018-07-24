## struts

Investigating the struts vulnerability exploit demonstrated in the
`java-goof` project.

 * `before.txt`: a normal GET request
 * `after.txt`: an exploit GET request
 * `diff.diff`: the difference in function sets between these two files.


## Suspicious?

The OgnlRuntime being triggered seems suspicious, but it's not the exploit:

 * `ognl/OgnlRuntime:classForName:classForName:(Ljava/lang/String;Ljava/util/Map;)Ljava/lang/Class;:185`
 * `ognl/OgnlRuntime:callStaticMethod:classForName:(Lognl/OgnlContext;Ljava/lang/String;)Ljava/lang/Class;:184`

Would need to inspect the arguments?
