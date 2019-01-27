package io.snyk.agent.filter;

import java.util.Optional;

/**
 * Match a class name, with optional method, in Javadoc notation, e.g. `com.example.Foo#bar()`.
 * <p>
 * The tests have examples.
 */
public class PathFilter {
    public final String className;
    public final boolean classNameIsPrefix;
    public final Optional<String> methodName;

    private PathFilter(String className, boolean classNameIsPrefix, Optional<String> methodName) {
        this.className = className;
        this.classNameIsPrefix = classNameIsPrefix;
        this.methodName = methodName;
    }

    public static PathFilter parse(String expression) {
        final String[] classMethod = expression.split("#");
        final Optional<String> methodName;
        if (1 == classMethod.length) {
            // no method specification
            methodName = Optional.empty();
        } else if (2 == classMethod.length) {
            methodName = Optional.of(classMethod[1]);
        } else {
            throw new IllegalStateException("multiple hashes in expression: " + expression);
        }

        String className = classMethod[0];
        final boolean classNameIsPrefix;
        if (className.endsWith("**")) {
            classNameIsPrefix = true;
            className = className.substring(0, className.length() - 2);
        } else {
            classNameIsPrefix = false;
        }

        if (className.contains("*")) {
            throw new IllegalStateException("further wildcards are not supported: " + expression);
        }

        return new PathFilter(className, classNameIsPrefix, methodName);
    }

    boolean testClass(final String actualClassName) {
        return classNameIsPrefix
                ? actualClassName.startsWith(className)
                : actualClassName.equals(className);
    }

    boolean testMethod(final String actualClassName, final String actualMethodName) {
        return testClass(actualClassName) && methodName.map(actualMethodName::equals).orElse(true);

    }
}
