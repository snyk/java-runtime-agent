package io.snyk.agent.logic;

import io.snyk.agent.filter.Filter;
import io.snyk.agent.util.InitLog;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * A parsed config (properties) file.
 */
public class Config {
    public final String projectId;
    public final AtomicReference<List<Filter>> filters;
    public final URI homeBaseUrl;
    public final long homeBasePostLimit;
    public final long startupDelayMs;
    public final long heartBeatIntervalMs;
    public final long reportIntervalMs;
    public final long filterUpdateIntervalMs;
    public final long filterUpdateInitialDelayMs;
    public final boolean trackClassLoading;
    public final LogDestinationConfig logTo;
    public final boolean debugLoggingEnabled;
    public final boolean trackBranchingMethods;
    public final boolean trackAccessors;
    final boolean skipBuiltInRules;
    public final boolean skipMetaPosts;

    private static final long DEFAULT_POST_LIMIT = 1024 * 1024;

    Config(String projectId,
           List<Filter> filters,
           String homeBaseUrl,
           Long homeBasePostLimit,
           long startupDelayMs,
           long heartBeatIntervalMs,
           long reportIntervalMs,
           long filterUpdateIntervalMs, long filterUpdateInitialDelayMs, boolean trackClassLoading,
           boolean trackAccessors,
           boolean trackBranchingMethods,
           String logTo,
           boolean debugLoggingEnabled,
           boolean skipBuiltInRules,
           boolean skipMetaPosts) {
        if (null == projectId) {
            throw new IllegalStateException("projectId is required");
        }
        this.projectId = projectId;
        if (filters.isEmpty()) {
            // unlikely: they should be using the non-empty built-in filters
            throw new IllegalStateException("no filters provided");
        }
        this.filters = new AtomicReference<>(Collections.unmodifiableList(filters));
        this.homeBaseUrl = URI.create(null != homeBaseUrl ? homeBaseUrl : "https://homebase.snyk.io/api/v1/");
        if (null == homeBasePostLimit) {
            this.homeBasePostLimit = DEFAULT_POST_LIMIT;
        } else {
            this.homeBasePostLimit = homeBasePostLimit;
        }
        this.startupDelayMs = startupDelayMs;
        this.heartBeatIntervalMs = heartBeatIntervalMs;
        this.reportIntervalMs = reportIntervalMs;
        this.filterUpdateIntervalMs = filterUpdateIntervalMs;
        this.filterUpdateInitialDelayMs = filterUpdateInitialDelayMs;
        this.trackClassLoading = trackClassLoading;
        this.trackAccessors = trackAccessors;
        this.logTo = LogDestinationConfig.fromNullableString(logTo);
        this.debugLoggingEnabled = debugLoggingEnabled;
        this.trackBranchingMethods = trackBranchingMethods;
        this.skipBuiltInRules = skipBuiltInRules;
        this.skipMetaPosts = skipMetaPosts;
    }

    public static Config fromFileWithDefault(String path) {
        try {
            InitLog.loading("loading config from: " + path);
            final ConfigBuilder intermediate = builderFromLines(Files.readAllLines(new File(path).toPath()));
            if (!intermediate.skipBuiltInRules) {
                InitLog.loading("adding built-in filters");
                intermediate.filters.addAll(loadBuiltInFilters());
            }
            return intermediate.build();
        } catch (IOException e) {
            InitLog.loading("error reading config file");
            throw new IllegalStateException(e);
        }
    }

    private static List<Filter> loadBuiltInFilters() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Config.class.getResourceAsStream(
                "/methods.properties")))) {
            return builderFromLines(reader.lines().collect(Collectors.toList())).filters;
        } catch (IOException builtin) {
            throw new IllegalStateException("built-in filter loading shouldn't fail", builtin);
        }
    }

    // @VisibleForTesting
    public static Config fromLinesWithoutDefault(String... lines) {
        return builderFromLines(Arrays.asList(lines)).build();
    }

    public static ConfigBuilder builderFromLines(Iterable<String> lines) {
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

            if ("logTo".equals(key)) {
                builder.logTo = value;
                continue;
            }

            if ("debugLoggingEnabled".equals(key)) {
                builder.debugLoggingEnabled = Boolean.parseBoolean(value);
                continue;
            }

            if ("skipMetaPosts".equals(key)) {
                builder.skipMetaPosts = Boolean.parseBoolean(value);
                continue;
            }

            if ("homeBaseUrl".equals(key)) {
                builder.homeBaseUrl = value;
                continue;
            }

            if ("skipBuiltInRules".equals(key)) {
                builder.skipBuiltInRules = Boolean.parseBoolean(value);
                continue;
            }

            if ("startupDelayMs".equals(key)) {
                builder.startupDelayMs = Long.parseLong(value);
                continue;
            }

            if ("heartBeatIntervalMs".equals(key)) {
                builder.heartBeatIntervalMs = Long.parseLong(value);
                continue;
            }

            if ("reportIntervalMs".equals(key)) {
                builder.reportIntervalMs = Long.parseLong(value);
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

        return builder;
    }
}
