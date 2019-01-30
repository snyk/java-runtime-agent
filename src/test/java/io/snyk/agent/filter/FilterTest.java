package io.snyk.agent.filter;

import io.snyk.agent.testutil.TestLogger;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class FilterTest {
    private static final TestLogger LOG = new TestLogger();

    @Test
    void testAllInArtifact() {
        final FilterList filter = new FilterList(Collections.singletonList(new Filter("foo",
                Optional.of("maven:io.snyk:snyk-agent"),
                Optional.empty(),
                Collections.singletonList(PathFilter.parse("**")))), Instant.EPOCH);

        assertNotEquals(Collections.emptyList(),
                filter.applicableFilters(LOG, Collections.emptyList(), "A"),
                "a class that we know nothing about the heritage of");
        assertNotEquals(Collections.emptyList(),
                filter.applicableFilters(LOG,
                Collections.singletonList("maven:io.snyk:snyk-agent:3.0"),
                "NotUsed"),
                "a class in the right artifact");
        assertNotEquals(Collections.emptyList(),
                filter.applicableFilters(LOG,
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
                        Collections.singletonList(PathFilter.parse("**")))), Instant.EPOCH);

        assertNotEquals(Collections.emptyList(),
                filters.applicableFilters(LOG, Collections.emptyList(), "NotUsed"),
                "a class that we know nothing about the heritage of");
        assertNotEquals(Collections.emptyList(),
                filters.applicableFilters(LOG,
                Collections.singletonList("maven:io.snyk:snyk-agent:2.0"),
                "NotUsed"),
                "a version which is too old");
        assertEquals(Collections.emptyList(),
                filters.applicableFilters(LOG,
                Collections.singletonList("maven:io.snyk:snyk-agent:3.0"),
                "NotUsed"),
                "a new enough version");
        assertEquals(Collections.emptyList(),
                filters.applicableFilters(LOG,
                Collections.singletonList("maven:io.snyk:snyk-agent:4.0"),
                "NotUsed"),
                "a new enough version");

        assertNotEquals(Collections.emptyList(),
                filters.applicableFilters(LOG,
                Arrays.asList("maven:io.snyk:snyk-agent:4.0", "maven:io.snyk:snyk-agent:2.0"), "NotUsed"),
                "a newer and older version both included");
    }
}
