package io.snyk.agent.filter;

import io.snyk.agent.testutil.TestLogger;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FilterTest {
    private static final TestLogger LOG = new TestLogger();

    @Test
    void testAllInArtifact() {
        final FilterList filter = new FilterList(Collections.singletonList(new Filter("foo",
                Optional.of("maven:io.snyk:snyk-agent"),
                Optional.empty(),
                Collections.singletonList(PathFilter.parse("**")))));

        assertTrue(filter.shouldProcessClass(LOG, Collections.emptyList(), "A"),
                "a class that we know nothing about the heritage of");
        assertTrue(filter.shouldProcessClass(LOG,
                Collections.singletonList("maven:io.snyk:snyk-agent:3.0"),
                "NotUsed"),
                "a class in the right artifact");
        assertTrue(filter.shouldProcessClass(LOG,
                Arrays.asList("maven:io.snyk:snyk-agent:3.0", "maven:foo.bar:baz:1.0"),
                "NotUsed"),
                "a class in the right artifact");
    }

    @Test
    void testVersionLessThan() {
        final FilterList filters = new FilterList(Collections.singletonList(
                new Filter("foo",
                        Optional.of("maven:io.snyk:snyk-agent"),
                        Optional.of(VersionFilter.parse("[,3)")),
                        Collections.singletonList(PathFilter.parse("**")))));

        assertTrue(filters.shouldProcessClass(LOG, Collections.emptyList(), "NotUsed"),
                "a class that we know nothing about the heritage of");
        assertTrue(filters.shouldProcessClass(LOG,
                Collections.singletonList("maven:io.snyk:snyk-agent:2.0"),
                "NotUsed"),
                "a version which is too old");
        assertFalse(filters.shouldProcessClass(LOG,
                Collections.singletonList("maven:io.snyk:snyk-agent:3.0"),
                "NotUsed"),
                "a new enough version");
        assertFalse(filters.shouldProcessClass(LOG,
                Collections.singletonList("maven:io.snyk:snyk-agent:4.0"),
                "NotUsed"),
                "a new enough version");

        assertTrue(filters.shouldProcessClass(LOG,
                Arrays.asList("maven:io.snyk:snyk-agent:4.0", "maven:io.snyk:snyk-agent:2.0"), "NotUsed"),
                "a newer and older version both included");
    }
}
