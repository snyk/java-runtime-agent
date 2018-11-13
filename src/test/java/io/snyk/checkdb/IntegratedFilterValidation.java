package io.snyk.checkdb;

import io.snyk.agent.filter.Filter;
import io.snyk.agent.filter.VersionFilter;
import io.snyk.agent.logic.Config;
import io.snyk.agent.util.IterableJar;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.apache.maven.index.ArtifactInfo;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.*;

class IntegratedFilterValidation {
    private static final Logger logger = LoggerFactory.getLogger(IntegratedFilterValidation.class);

    @Test
    void validateFilters() throws Exception {
        Assumptions.assumeTrue(Boolean.valueOf(System.getenv("VALIDATE_FILTERS")));
        final Config config = defaultConfig();
        try (final MavenIndex index = new MavenIndex()) {
            index.maybeUpdateIndex();

            for (Filter filter : config.filters) {
                MDC.put("filter", debugKey(filter));
                logger.debug("Checking");
                final String stringArtifact = filter.artifact.get();
                final String[] parts = stringArtifact.split(":", 3);
                final List<String> versions = new ArrayList<>(20);
                final String groupId = parts[1];
                final String artifactId = parts[2];
                for (ArtifactInfo ai : index.find(groupId, artifactId)) {
                    versions.add(ai.getVersion());
                }
                final String version = findCandidateVersion(filter, versions);

                final File path = index.fetch(groupId, artifactId, version);
                matchingClassesInJar(path, filter);
            }
        }
    }

    private void matchingClassesInJar(File path, Filter filter) {
        final IterableJar jar = new IterableJar(() -> {
            try {
                return new FileInputStream(path);
            } catch (FileNotFoundException e) {
                throw new IllegalStateException(e);
            }
        });

        final List<String> jarContains = StreamSupport.stream(jar.spliterator(), false)
                .map(node -> node.name)
                .collect(Collectors.toList());

        final List<String> matches = jarContains
                .stream()
                .filter(filter::testClassName)
                .collect(Collectors.toList());

        assertTrue(!matches.isEmpty(),
                debugKey(filter) + "\nexpects a class matching:\n   "
                        + filter.pathFilters
                        + "\n  in: " + path
                        + "\n  but none do:\n   * " + String.join("\n   * ", jarContains));

        assertEquals(1, matches.size(), debugKey(filter) + "expected exactly one class, not: " + matches);
    }

    private String findCandidateVersion(Filter filter, List<String> versions) {
        assertFalse(versions.isEmpty(), debugKey(filter) + "no matching artifact:group");

        final VersionFilter versionFilter = filter.version.get();
        final List<String> matchingVersions = versions.stream()
                .filter(versionFilter)
                .collect(Collectors.toList());

        assertFalse(matchingVersions.isEmpty(),
                debugKey(filter) + "none of the versions match:\n * " + String.join("\n * ", versions));

        return matchingVersions.stream().max(Comparator.comparing(ComparableVersion::new)).get();
    }

    private String debugKey(Filter filter) {
        return "filter " + filter.name + " (" + filter.artifact + "): ";
    }

    private Config defaultConfig() throws IOException {
        final File emptyFile = File.createTempFile("config", ".properties");
        Files.write(emptyFile.toPath(), Collections.singletonList(
                "projectId=d666464d-ab0a-43c2-bc5d-448d6bdc311b"
        ));
        try {
            return Config.fromFileWithDefault(emptyFile.getAbsolutePath());
        } finally {
            assertTrue(emptyFile.delete());
        }
    }
}
