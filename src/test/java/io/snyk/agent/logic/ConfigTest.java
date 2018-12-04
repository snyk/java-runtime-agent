package io.snyk.agent.logic;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class ConfigTest {
    @Test
    void loadFilterFreeFileAndGetDefaultFilters() throws IOException {
        loadConfig(Collections.singletonList(
                "projectId=a9fe5eb7-88f1-43da-86ae-9b7b15d9049d"
        ), config -> assertNotEquals(0, config.filters.get().size()));
    }

    @Test
    void skipFilters() throws IOException {
        loadConfig(Arrays.asList(
                "projectId=a9fe5eb7-88f1-43da-86ae-9b7b15d9049d",
                "filter.foo.paths = something/must/Be#provided",
                "skipBuiltInRules = true"
        ), config -> assertEquals(1, config.filters.get().size()));
    }

    private void loadConfig(List<String> lines, Consumer<Config> check) throws IOException {
        final File emptyFile = File.createTempFile("config", ".properties");
        Files.write(emptyFile.toPath(), lines);
        try {
            final Config config = Config.fromFileWithDefault(emptyFile.getAbsolutePath());

            check.accept(config);
        } finally {
            assertTrue(emptyFile.delete());
        }
    }
}
