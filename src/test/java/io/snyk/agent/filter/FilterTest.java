package io.snyk.agent.filter;

import io.snyk.agent.testutil.TestLogger;
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
                Optional.of("maven:io.snyk:snyk-agent"),
                Optional.empty(),
                Collections.emptyList());

        assertTrue(filter.testArtifacts(new TestLogger(), Collections.emptyList()),
                "a class that we know nothing about the heritage of");
        assertTrue(filter.testArtifacts(new TestLogger(), Collections.singletonList("maven:io.snyk:snyk-agent:3.0")),
                "a class in the right artifact");
        assertTrue(filter.testArtifacts(new TestLogger(), Arrays.asList("maven:io.snyk:snyk-agent:3.0", "maven:foo.bar:baz:1.0")),
                "a class in the right artifact");
    }

    @Test
    void testVersionLessThan() {
        final Filter filter = new Filter("foo",
                Optional.of("maven:io.snyk:snyk-agent"),
                Optional.of(VersionFilter.parse("<3")),
                Collections.emptyList());

        assertTrue(filter.testArtifacts(new TestLogger(), Collections.emptyList()),
                "a class that we know nothing about the heritage of");
        assertTrue(filter.testArtifacts(new TestLogger(), Collections.singletonList("maven:io.snyk:snyk-agent:2.0")),
                "a version which is too old");
        assertFalse(filter.testArtifacts(new TestLogger(), Collections.singletonList("maven:io.snyk:snyk-agent:3.0")),
                "a new enough version");
        assertFalse(filter.testArtifacts(new TestLogger(), Collections.singletonList("maven:io.snyk:snyk-agent:4.0")),
                "a new enough version");

        assertTrue(filter.testArtifacts(new TestLogger(),
                Arrays.asList("maven:io.snyk:snyk-agent:4.0", "maven:io.snyk:snyk-agent:2.0")),
                "a newer and older version both included");
    }
}
