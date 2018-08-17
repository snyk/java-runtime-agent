package io.snyk.agent.filter;

import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A stub implementation that only supports "'major' version is less than"
 */
public class VersionFilter implements Predicate<String> {

    private static final Pattern LESS_THAN_SINGLE_NUMBER = Pattern.compile("<(\\d+)");

    private final long major;

    VersionFilter(long major) {
        this.major = major;
    }

    public static VersionFilter parse(String expression) {
        final Matcher ma = LESS_THAN_SINGLE_NUMBER.matcher(expression);
        if (!ma.matches()) {
            throw new IllegalStateException("unsupported version expression: " + expression);
        }

        return new VersionFilter(Long.parseLong(ma.group(1)));
    }

    @Override
    public boolean test(String s) {
        final String[] parts = s.split("[.,~-]");
        try {
            return Long.parseLong(parts[0]) < major;
        } catch (NumberFormatException e) {
            // non-numeric version number, assume it matches?
            return true;
        }
    }
}
