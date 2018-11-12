package io.snyk.checkdb;

import io.snyk.agent.filter.Filter;
import io.snyk.agent.filter.VersionFilter;
import io.snyk.agent.logic.Config;
import io.snyk.agent.util.IterableJar;
import org.apache.maven.index.ArtifactInfo;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.*;

class IntegratedFilterValidation {
    @Test
    void validateFilters()
            throws PlexusContainerException, ComponentLookupException, IOException, InterruptedException {
        final Config config = defaultConfig();
        try (final MavenIndex index = new MavenIndex()) {
            index.maybeUpdateIndex();

            for (Filter filter : config.filters) {
                final String stringArtifact = filter.artifact.get();
                final String[] parts = stringArtifact.split(":", 3);
                final List<String> versions = new ArrayList<>(20);
                final String groupId = parts[1];
                final String artifactId = parts[2];
                for (ArtifactInfo ai : index.find(groupId, artifactId)) {
                    versions.add(ai.getVersion());
                }
                final String version = findCandidateVersion(filter, versions);

                System.out.println("considering " + version);
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
                debugKey(filter) + "\nexpects a class matching " + filter.pathFilters
                        + " but none do:\n * " + String.join("\n * ", jarContains));

        assertEquals(1, matches.size(), debugKey(filter) + "expected exactly one class, not: " + matches);
    }

    private String findCandidateVersion(Filter filter, List<String> versions) {
        assertFalse(versions.isEmpty(), debugKey(filter) + "no matching artifact:group");

        final VersionFilter versionFilter = filter.version.get();
        final List<String> matchingVersions = versions.stream()
                .filter(versionFilter)
                .collect(Collectors.toList());

        System.out.println(debugKey(filter) + matchingVersions.size() + "/" + versions.size() + " match");

        assertFalse(matchingVersions.isEmpty(), debugKey(filter) + "none of the versions match");

        // Don't really care which version here. Would like to pick a random one? Or all of them?
        // Lexicographically first is close enough to random for me.
        Collections.sort(matchingVersions);
        return matchingVersions.get(0);
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
