package io.snyk.agent.logic;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class Config {
    public final String projectId;
    public final Map<String, Filter> filter;
    public final String urlPrefix;

    Config(String projectId, Map<String, Filter> filter, String urlPrefix) {
        this.projectId = null != projectId ? projectId : "no-project-id-provided";
        this.filter = filter;
        this.urlPrefix = null != urlPrefix ? urlPrefix : "http://localhost:8000";
    }

    public static Config fromFile(String path) {
        try {
            return fromLines(Files.readAllLines(new File(path).toPath()));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Config fromLines(Iterable<String> lines) {
        String projectId = null;
        Map<String, Filter> filters = new HashMap<>();
        String urlPrefix = null;

        // this looks awfully like a .properties file. Maybe it could be a .properties file?
        // .properties is awful at unicode and multi-value, but we probably don't care

        for (String line : lines) {
            if (line.startsWith("#")) {
                continue;
            }

            final String stripped = line.trim();

            if (stripped.isEmpty()) {
                continue;
            }

            final String[] splitUp = stripped.split("\\s*=\\s*", 2);
            final String key = splitUp[0];
            final String value = splitUp[1];

            final String filterName;

            if ("projectId".equals(key)) {
                projectId = value;
            } else if ("urlPrefix".equals(key)) {
                urlPrefix = value;
            } else if (hasPrefix(key, "filterArtifact", filters, o -> o.artifact = value)) {
                // TODO: there'll be a better way todo this in the morning, I bet.
            } else if (hasPrefix(key, "filterVersion", filters, o -> o.version = value)) {
            } else if (hasPrefix(key, "filterPaths", filters, o -> o.addPathsFrom(value))) {
            } else {
                System.err.println("snyk-agent: unrecognised key: " + key);
            }
        }

        return new Config(projectId, filters, urlPrefix);
    }

    private static boolean hasPrefix(String from,
                                     String prefix,
                                     Map<String, Filter> filters,
                                     Consumer<Filter> updateFilter) {
        if (!from.startsWith(prefix)) {
            return false;
        }

        updateFilter.accept(filters.computeIfAbsent(from.substring(prefix.length()), name -> new Filter(name)));

        return true;
    }
}
