package io.snyk.agent.filter;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FilterTest {

    @Test
    void testAllInArtfiact() {
        final Filter filter = new Filter("foo",
                Optional.of("io.snyk:snyk-agent"),
                Optional.empty(),
                Collections.emptyList());

        assertTrue(filter.test(Collections.emptyList(), "foo.bar.Baz#quux()"),
                "a class that we know nothing about the heritage of");
        assertTrue(filter.test(Collections.singletonList("io.snyk:snyk-agent:3.0"), "foo.bar.Baz#quux()"),
                "a class in the right artifact");
        assertTrue(filter.test(Arrays.asList("io.snyk:snyk-agent:3.0", "foo.bar:baz:1.0"), "foo.bar.Baz#quux()"),
                "a class in the right artifact");
    }

    @Test
    void testVersionLessThan() {
        final Filter filter = new Filter("foo",
                Optional.of("io.snyk:snyk-agent"),
                Optional.of(new VersionFilter(3)),
                Collections.emptyList());

        assertTrue(filter.test(Collections.emptyList(), "foo.bar.Baz#quux()"),
                "a class that we know nothing about the heritage of");
        assertTrue(filter.test(Collections.singletonList("io.snyk:snyk-agent:2.0"), "foo.bar.Baz#quux()"),
                "a version which is too old");
        assertFalse(filter.test(Collections.singletonList("io.snyk:snyk-agent:3.0"), "foo.bar.Baz#quux()"),
                "a new enough version");
        assertFalse(filter.test(Collections.singletonList("io.snyk:snyk-agent:4.0"), "foo.bar.Baz#quux()"),
                "a new enough version");

        assertTrue(filter.test(Arrays.asList("io.snyk:snyk-agent:4.0", "io.snyk:snyk-agent:2.0"), "foo.bar.Baz#quux()"),
                "a newer and older version both included");
    }
}
