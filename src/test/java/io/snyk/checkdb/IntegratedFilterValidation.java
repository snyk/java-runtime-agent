package io.snyk.checkdb;

import io.snyk.agent.filter.Filter;
import io.snyk.agent.filter.VersionFilter;
import io.snyk.agent.logic.Config;
import org.apache.maven.index.ArtifactInfo;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

                index.fetch(groupId, artifactId, version);
            }
        }
    }

    private String findCandidateVersion(Filter filter, List<String> versions) {
        final String debugKey = "filter " + filter.name + " (" + filter.artifact + "): ";
        assertFalse(versions.isEmpty(), debugKey + "no matching artifact:group");

        final VersionFilter versionFilter = filter.version.get();
        final List<String> matchingVersions = versions.stream()
                .filter(versionFilter)
                .collect(Collectors.toList());

        System.out.println(debugKey + matchingVersions.size() + "/" + versions.size() + " match");

        assertFalse(matchingVersions.isEmpty(), debugKey + "none of the versions match");

        // Don't really care which version here. Would like to pick a random one? Or all of them?
        // Lexicographically first is close enough to random for me.
        Collections.sort(matchingVersions);
        return matchingVersions.get(0);
    }

    private Config defaultConfig() throws IOException {
        final File emptyFile = File.createTempFile("config", ".properties");
        Files.write(emptyFile.toPath(), Collections.singletonList(
                "projectId=d666464d-ab0a-43c2-bc5d-448d6bdc311b"
        ));
        final Config config;
        try {
            config = Config.fromFileWithDefault(emptyFile.getAbsolutePath());
        } finally {
            assertTrue(emptyFile.delete());
        }
        return config;
    }
}
