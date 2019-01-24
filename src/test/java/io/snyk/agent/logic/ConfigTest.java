package io.snyk.agent.logic;

import io.snyk.agent.filter.FilterList;
import io.snyk.agent.testutil.TestLogger;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigTest {
    @Test
    void loadJustProjectId() throws IOException {
        loadConfig(Arrays.asList(
                "projectId=a9fe5eb7-88f1-43da-86ae-9b7b15d9049d"
        ), config -> assertEquals("a9fe5eb7-88f1-43da-86ae-9b7b15d9049d", config.projectId));
    }

    private void loadConfig(List<String> lines, Consumer<Config> check) throws IOException {
        final File file = File.createTempFile("config", ".properties");
        Files.write(file.toPath(), lines);
        try {
            check.accept(Config.loadConfigFromFile(file.getAbsolutePath()));
        } finally {
            assertTrue(file.delete());
        }
    }

    public static Config makeConfig(Iterable<String> configLines, Iterable<String> filterLines) {
        final Config config = Config.loadConfig(configLines);
        config.filters.set(FilterList.loadFiltersFrom(new TestLogger(), filterLines));
        return config;
    }
}
