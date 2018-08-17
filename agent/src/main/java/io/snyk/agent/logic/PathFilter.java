package io.snyk.agent.logic;

import java.util.function.Predicate;

public class PathFilter implements Predicate<String> {
    private final String className;
    private final boolean classNameIsPrefix;

    // @Nullable
    private final String methodName;

    private PathFilter(String className, boolean classNameIsPrefix, String methodName) {
        this.className = className;
        this.classNameIsPrefix = classNameIsPrefix;
        this.methodName = methodName;
    }

    public static PathFilter parse(String expression) {
        final String[] classMethod = expression.split("#");
        final String methodName;
        if (1 == classMethod.length) {
            // no method specification
            methodName = null;
        } else if (2 == classMethod.length) {
            methodName = classMethod[1];
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

    @Override
    public boolean test(final String input) {
        final String[] parts = input.split("#|\\(", 3);
        final String actualClassName = parts[0];
        final String actualMethodName = parts[1];

        if (classNameIsPrefix) {
            if (!actualClassName.startsWith(className)) {
                return false;
            }
        } else {
            if (!actualClassName.equals(className)) {
                return false;
            }
        }

        if (null != methodName) {
            if (!actualMethodName.equals(methodName)) {
                return false;
            }
        }

        return true;
    }
}
