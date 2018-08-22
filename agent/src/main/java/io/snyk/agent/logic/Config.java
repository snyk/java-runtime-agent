package io.snyk.agent.logic;

import io.snyk.agent.filter.Filter;
import io.snyk.agent.util.Log;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Config {
    public final String projectId;
    public final List<Filter> filters;
    public final String urlPrefix;

    Config(String projectId, List<Filter> filters, String urlPrefix) {
        this.projectId = null != projectId ? projectId : "no-project-id-provided";
        this.filters = filters;
        this.urlPrefix = null != urlPrefix ? urlPrefix : "http://localhost:8000";
    }

    public static Config fromFile(String path) {
        try {
            return fromLines(Files.readAllLines(new File(path).toPath()));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static Config fromLines(Iterable<String> lines) {
        String projectId = null;
        Map<String, Filter.Builder> filters = new HashMap<>();
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

            if ("projectId".equals(key)) {
                projectId = value;
                continue;
            }

            if ("urlPrefix".equals(key)) {
                urlPrefix = value;
                continue;
            }

            if (key.startsWith("filter.")) {
                final String[] parts = key.split("\\.", 3);
                if (3 != parts.length) {
                    Log.loading("invalid filter. key: " + key);
                    continue;
                }

                final String filterName = parts[1];
                final String filterCommand = parts[2];

                final Filter.Builder filter = filters.computeIfAbsent(filterName, Filter.Builder::new);

                switch (filterCommand) {
                    case "artifact":
                        filter.artifact = value;
                        break;
                    case "version":
                        filter.version = value;
                        break;
                    case "paths":
                        filter.addPathsFrom(value);
                        break;
                    default:
                        Log.loading("unrecognised filter command: " + key);
                }

                continue;
            }

            Log.loading("unrecognised key: " + key);
        }

        return new Config(projectId,
                filters.values().stream().map(Filter.Builder::build).collect(Collectors.toList()),
                urlPrefix);
    }
}
