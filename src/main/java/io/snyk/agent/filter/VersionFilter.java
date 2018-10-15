package io.snyk.agent.filter;

import io.snyk.agent.util.org.apache.maven.artifact.versioning.ComparableVersion;

import java.util.function.Predicate;

/**
 * Stub implementation: only supports <, > or = a version.
 *
 * Note that versions are compared using the Maven version syntax, not semver,
 * so versions with hyphens and letters (and all kinds of other horrors) in will work.
 */
public class VersionFilter implements Predicate<String> {

    public final ComparableVersion version;
    public final int direction;

    private VersionFilter(ComparableVersion version, int direction) {
        this.version = version;
        this.direction = direction;
    }

    public static VersionFilter parse(String expression) {
        if (expression.length() < 2) {
            throw new IllegalStateException("too short: " + expression);
        }

        final int direction;

        switch (expression.charAt(0)) {
            case '<':
                direction = -1;
                break;
            case '>':
                direction = 1;
                break;
            case '=':
                direction = 0;
                break;
            default:
                throw new IllegalStateException("version expression must start with </>/=");
        }

        final ComparableVersion version = new ComparableVersion(expression.substring(1).trim());

        return new VersionFilter(version, direction);
    }

    @Override
    public boolean test(String s) {
        return direction == new ComparableVersion(s).compareTo(version);
    }
}
