package io.snyk.agent.logic;

import io.snyk.agent.filter.Filter;
import io.snyk.agent.util.InitLog;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A parsed config (properties) file.
 */
public class Config {
    public final String projectId;
    public final List<Filter> filters;
    public final URI homeBaseUrl;
    public final long homeBasePostLimit;
    public final boolean trackClassLoading;
    public final boolean debugLoggingEnabled;
    public final boolean trackBranchingMethods;
    public final boolean trackAccessors;

    private static final long DEFAULT_POST_LIMIT = 1024 * 1024;

    Config(String projectId,
           List<Filter> filters,
           String homeBaseUrl,
           Long homeBasePostLimit,
           boolean trackClassLoading,
           boolean trackAccessors,
           boolean trackBranchingMethods,
           boolean debugLoggingEnabled) throws MalformedURLException {
        if (null == projectId) {
            throw new IllegalStateException("projectId is required");
        }
        this.projectId = projectId;
        this.filters = Collections.unmodifiableList(filters);
        this.homeBaseUrl = URI.create(null != homeBaseUrl ? homeBaseUrl : "https://homebase.snyk.io/api/v1/beacon");
        if (null == homeBasePostLimit) {
            this.homeBasePostLimit = DEFAULT_POST_LIMIT;
        } else {
            this.homeBasePostLimit = homeBasePostLimit;
        }
        this.trackClassLoading = trackClassLoading;
        this.trackAccessors = trackAccessors;
        this.debugLoggingEnabled = debugLoggingEnabled;
        this.trackBranchingMethods = trackBranchingMethods;
    }

    public static Config fromFile(String path) {
        try {
            InitLog.loading("loading config from: " + path);
            return fromLines(Files.readAllLines(new File(path).toPath()));
        } catch (IOException e) {
            InitLog.loading("error reading config file");
            throw new IllegalStateException(e);
        }
    }

    public static Config fromLines(Iterable<String> lines) throws MalformedURLException {
        final ConfigBuilder builder = new ConfigBuilder();
        final Map<String, Filter.Builder> filters = new HashMap<>();

        // this looks awfully like a .properties file. Maybe it could use a real properties loader?
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
            if (splitUp.length < 2) {
                InitLog.loading("invalid line: " + stripped);
                continue;
            }

            final String key = splitUp[0];
            final String value = splitUp[1];

            if ("projectId".equals(key)) {
                builder.projectId = value;
                continue;
            }

            if ("trackAccessors".equals(key)) {
                builder.trackAccessors = Boolean.parseBoolean(value);
                continue;
            }

            if ("trackClassLoading".equals(key)) {
                builder.trackClassLoading = Boolean.parseBoolean(value);
                continue;
            }

            if ("trackBranchingMethods".equals(key)) {
                builder.trackBranchingMethods = Boolean.parseBoolean(value);
                continue;
            }

            if ("debugLoggingEnabled".equals(key)) {
                builder.debugLoggingEnabled = Boolean.parseBoolean(value);
                continue;
            }

            if ("homeBaseUrl".equals(key)) {
                builder.homeBaseUrl = value;
                continue;
            }

            if (key.startsWith("filter.")) {
                final String[] parts = key.split("\\.", 3);
                if (3 != parts.length) {
                    InitLog.loading("invalid filter. key: " + key);
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
                        InitLog.loading("unrecognised filter command: " + key);
                }

                continue;
            }

            InitLog.loading("unrecognised key: " + key);
        }

        builder.filters = filters.values().stream().map(Filter.Builder::build).collect(Collectors.toList());

        return builder.build();
    }
}
