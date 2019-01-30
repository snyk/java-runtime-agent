package io.snyk.agent.filter;

import io.snyk.agent.util.org.apache.maven.artifact.versioning.ComparableVersion;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Note that versions are compared using the Maven version syntax, not semver,
 * so versions with hyphens and letters (and all kinds of other horrors) in will work.
 */
public class VersionFilter implements Predicate<String> {

    // anything except comma, ], ), and space; our delimiters.
    private static final String VERSION_PART_REGEX = "([^,\\])\\s]*)";

    // e.g. [3,)
    // e.g. (3,5]
    // e.g. (,7)
    private static final Pattern VERSION_EXPRESSION = Pattern.compile(
            "([\\[(])" + // [ or (
                    VERSION_PART_REGEX + // the first version bit
                    "\\s*,\\s*" + // comma
                    VERSION_PART_REGEX + // the second version bit
                    "([\\])]),*" // ] or ), and zero or more trailing commas
    );

    private final List<Predicate<ComparableVersion>> options;

    private VersionFilter(List<Predicate<ComparableVersion>> options) {
        this.options = options;
    }

    public static VersionFilter parse(String expression) {
        final List<Predicate<ComparableVersion>> options = new ArrayList<>();

        for (String part : expression.split("\\s+")) {
            final Matcher ma = VERSION_EXPRESSION.matcher(part);
            if (!ma.matches()) {
                throw new IllegalStateException("invalid version expression: " + part);
            }

            final boolean startClosed = ma.group(1).equals("[");
            final String startExpr = ma.group(2);
            final String endExpr = ma.group(3);
            final boolean endClosed = ma.group(4).equals("]");

            final Optional<ComparableVersion> startVersion = versionOrEmpty(startExpr);
            final Optional<ComparableVersion> endVersion = versionOrEmpty(endExpr);

            options.add(candidate ->
                    startVersion.map(version -> greaterThan(startClosed, candidate, version)).orElse(true) &&
                            endVersion.map(version -> greaterThan(endClosed, version, candidate)).orElse(true));
        }

        return new VersionFilter(options);
    }

    private static boolean greaterThan(boolean orEqualTo, ComparableVersion left, ComparableVersion right) {
        final int comparison = left.compareTo(right);
        if (orEqualTo) {
            return comparison >= 0;
        } else {
            return comparison > 0;
        }
    }

    private static Optional<ComparableVersion> versionOrEmpty(String expr) {
        if (expr.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new ComparableVersion(expr));
    }

    @Override
    public boolean test(String version) {
        final ComparableVersion comparableVersion = new ComparableVersion(version);
        return options.stream().anyMatch(p -> p.test(comparableVersion));
    }
}
