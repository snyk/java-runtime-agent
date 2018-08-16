## v0.0 metadata

 * timestamp of method call
 * methodName
 * moduleVersion
 * moduleBaseDir
 * moduleName
 * moduleScriptRelativePath

## v0.1 metadata
 * timestamp of ... the submission? Seems unnecessary, for http.
 * potentially empty set of .. `module locators`:
   * e.g. `()`
   * e.g. `("npm:st:0.2.4")`
   * e.g. `("maven:org.apache:commons-lang3:3.0", "maven:com.google:guava:26.0-jre~20180203.000129")`
 * path/url where the code was loaded from, if available:
   * e.g. `file:/Users/assafhefetz/development/goof/node_modules/st/st.js:158:0`
   * e.g. `file:/usr/share/maven/lib/maven-settings-builder-3.x.jar`
   * e.g. `http://localhost/foo.jar`
   * e.g. `jar:file:/usr/share/maven/lib/guava.jar!/com/google/common/collect/NullsLastOrdering.class`
   * e.g. `null`
 * identifier for the method (ignoring lambdas / synthetics)
   * e.g. `getPath`
   * e.g. `org.apache.catalina.core.ContainerBase#getMappingObject(Ljava.lang.Object;, int, int)` (stick to the `fourSignature`?)
 * extra info, entirely optional?
   * line number
   * `fiveSignature` (see below)
   * `baseDir`, `scriptRelativePath`?

## terms

 * "always": unless something horrible goes wrong (99.9% of the time)
 * "typically": unless someone has intentionally messed with us (95% of the time)
 * "frequently": in a real deployment, it'd probably work (80% of the time)
 * "sometimes": available with naive, positive defaults (60% of the time)

## node overview

`(moduleName, moduleVersion, methodName)` is TYPICALLY globally unique.

e.g. (`st`, `0.2.4`, `getPath`).

Other information is there to help the user understand their deployment, or for us
to debug our agent. They are TYPICALLY available, and probably make sense to users.

e.g.

 * `bp`: `/Users/assafhefetz/development/goof/node_modules/st/st.js:158:0`
 * `baseDir`: `/Users/assafhefetz/development/goof/node_modules/st`
 * `scriptRelativePath`: `st.js`

Is the line number in the `bp` required, or is the method name globally unique?
i.e. is there no other namespacing in node?

## java overview

Java code is ALWAYS in a method.

A method ALWAYS has a `className`, and FREQUENTLY has a `methodName`. (think lambdas, implementation details)

TYPICALLY a `className` allows you to find the library, but not the version.

A `className` is ALWAYS loaded by a `ClassLoader`, who TYPICALLY don't have useful names.

A `className` is TYPICALLY loaded from a `URL`, which is TYPICALLY a `jar file`.

A `jar file` SOMETIMES has Maven metadata in, including zero-or-more
 `artifactGroup`, `artifactName` and `artifactVersion`s.


Info we TYPICALLY have available:

 * `className`, e.g. `org.apache.catalina.core.ContainerBase`
 * `methodName`, e.g. `getMappingObject`.
 * `fourSignature`, e.g. "this function takes `(Object, int, int)`", which is enough to resolve overloads.
 * `URL` from which the class was loaded.

Info we SOMETIMES have available:

 * Some "maven triplet" (`artifactGroup`, `artifactName`, `artifactVersion`) for the `jar file`
    the `className` was loaded from.
 * `fiveSignature`, e.g. "this function takes `(List<String> parameters, int start, int end)`".
 * `lineNumber` of the first line of the method, or of the call-site.
