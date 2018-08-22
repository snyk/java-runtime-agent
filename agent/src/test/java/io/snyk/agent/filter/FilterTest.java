package io.snyk.agent.filter;

import io.snyk.agent.util.Log;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FilterTest {

    @Test
    void testAllInArtifact() {
        final Filter filter = new Filter("foo",
                Optional.of("io.snyk:snyk-agent"),
                Optional.empty(),
                Collections.emptyList());

        assertTrue(filter.testArtifacts(new Log(), Collections.emptyList()),
                "a class that we know nothing about the heritage of");
        assertTrue(filter.testArtifacts(new Log(), Collections.singletonList("io.snyk:snyk-agent:3.0")),
                "a class in the right artifact");
        assertTrue(filter.testArtifacts(new Log(), Arrays.asList("io.snyk:snyk-agent:3.0", "foo.bar:baz:1.0")),
                "a class in the right artifact");
    }

    @Test
    void testVersionLessThan() {
        final Filter filter = new Filter("foo",
                Optional.of("io.snyk:snyk-agent"),
                Optional.of(VersionFilter.parse("<3")),
                Collections.emptyList());

        assertTrue(filter.testArtifacts(new Log(), Collections.emptyList()),
                "a class that we know nothing about the heritage of");
        assertTrue(filter.testArtifacts(new Log(), Collections.singletonList("io.snyk:snyk-agent:2.0")),
                "a version which is too old");
        assertFalse(filter.testArtifacts(new Log(), Collections.singletonList("io.snyk:snyk-agent:3.0")),
                "a new enough version");
        assertFalse(filter.testArtifacts(new Log(), Collections.singletonList("io.snyk:snyk-agent:4.0")),
                "a new enough version");

        assertTrue(filter.testArtifacts(new Log(),
                Arrays.asList("io.snyk:snyk-agent:4.0", "io.snyk:snyk-agent:2.0")),
                "a newer and older version both included");
    }
}
