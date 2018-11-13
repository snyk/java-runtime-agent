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

public class Checker {
    @Test
    public void check() throws PlexusContainerException, ComponentLookupException, IOException {
        final Config config = defaultConfig();
        try (final MavenIndex index = new MavenIndex()) {
            index.maybeUpdateIndex();

            for (Filter filter : config.filters) {
                final String stringArtifact = filter.artifact.get();
                final String[] parts = stringArtifact.split(":", 3);
                final List<String> versions = new ArrayList<>(20);
                for (ArtifactInfo ai : index.find(parts[1], parts[2])) {
                    versions.add(ai.getVersion());
                }
                check(filter, versions);
            }
        }
    }

    void check(Filter filter, List<String> versions) {
        final String debugKey = "filter " + filter.name + " (" + filter.artifact + "): ";
        assertFalse(versions.isEmpty(), debugKey + "no matching artifact:group");

        final VersionFilter versionFilter = filter.version.get();
        final List<String> matchingVersions = versions.stream()
                .filter(versionFilter::test)
                .collect(Collectors.toList());

        System.out.println(debugKey + matchingVersions.size() + "/" + versions.size() + " match");

        assertFalse(matchingVersions.isEmpty(), debugKey + "none of the versions match");
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
