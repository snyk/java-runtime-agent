package io.snyk.agent.filter;

import java.util.Optional;
import java.util.function.Predicate;

public class PathFilter implements Predicate<String> {
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

    public boolean testClass(final String actualClassName) {
        return classNameIsPrefix
                ? actualClassName.startsWith(className)
                : actualClassName.equals(className);
    }

    @Override
    public boolean test(final String input) {
        final String[] parts = input.split("#|\\(", 3);
        final String actualClassName = parts[0];
        final String actualMethodName = parts[1];

        if (!testClass(actualClassName)) {
            return false;
        }

        if (methodName.isPresent()) {
            return actualMethodName.equals(methodName.get());
        }

        return true;
    }
}
