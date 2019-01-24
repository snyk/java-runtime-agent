package io.snyk.agent.logic;

import io.snyk.agent.filter.Filter;
import io.snyk.agent.filter.FilterList;
import io.snyk.agent.util.InitLog;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * A parsed config (properties) file.
 */
public class Config {
    public final String projectId;
    public final AtomicReference<FilterList> filters;
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
        this.filters = new AtomicReference<>(new FilterList(filters));
        this.homeBaseUrl = URI.create(null != homeBaseUrl ? homeBaseUrl : "https://homebase.snyk.io/api/");
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

    private static List<Filter> loadBuiltInFiltersFromResource(String resourceName, boolean shouldThrow) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Config.class.getResourceAsStream(
                resourceName)))) {
            return builderFromLines(reader.lines().collect(Collectors.toList())).filters;
        } catch (Exception error) {
            if (shouldThrow) {
                throw new IllegalStateException(String.format("built-in filter loading shouldn't fail from resource %s",
                        resourceName), error);
            }

            InitLog.loading(String.format("Failed loading snaphost from resource %s", resourceName));
            return null;
        }
    }

    private static List<Filter> loadBuiltInFilters() {
        try {
            InitLog.loading("loading built-in filters from bundled snapshot");
            List<Filter> builtInFilters = loadBuiltInFiltersFromResource("/methods.bundled.properties", false);
            if (builtInFilters == null || builtInFilters.size() == 0) {
                throw new Exception("Invalid bundled snaphost format");
            }
            return builtInFilters;
        } catch (Exception error) {
            InitLog.loading("Failed loading bundled snaphost, falling back to the snapshot provided in the repo");
        }

        return loadBuiltInFiltersFromResource("/methods.properties", true);
    }

    // @VisibleForTesting
    public static Config fromLinesWithoutDefault(String... lines) {
        return builderFromLines(Arrays.asList(lines)).build();
    }

    private static Map<String, String> loadConfig(Iterable<String> lines) {
        // this looks awfully like a .properties file. Maybe it could use a real properties loader?
        // .properties is awful at unicode and multi-value, but we probably don't care

        final Map<String, String> ret = new HashMap<>();
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

            ret.put(splitUp[0], splitUp[1]);
        }

        return ret;
    }

    static ConfigBuilder builderFromLines(Iterable<String> lines) {
        final ConfigBuilder builder = new ConfigBuilder();
        final Map<String, Filter.Builder> filters = new HashMap<>();

        loadConfig(lines).forEach((key, value) -> {
            switch (key) {
                case "projectId":
                    builder.projectId = value;
                    return;

                case "trackAccessors":
                    builder.trackAccessors = Boolean.parseBoolean(value);
                    return;

                case "trackClassLoading":
                    builder.trackClassLoading = Boolean.parseBoolean(value);
                    return;

                case "trackBranchingMethods":
                    builder.trackBranchingMethods = Boolean.parseBoolean(value);
                    return;

                case "logTo":
                    builder.logTo = value;
                    return;

                case "debugLoggingEnabled":
                    builder.debugLoggingEnabled = Boolean.parseBoolean(value);
                    return;

                case "skipMetaPosts":
                    builder.skipMetaPosts = Boolean.parseBoolean(value);
                    return;

                case "homeBaseUrl":
                    builder.homeBaseUrl = value;
                    return;

                case "skipBuiltInRules":
                    builder.skipBuiltInRules = Boolean.parseBoolean(value);
                    return;

                case "startupDelayMs":
                    builder.startupDelayMs = Long.parseLong(value);
                    return;

                case "heartBeatIntervalMs":
                    builder.heartBeatIntervalMs = Long.parseLong(value);
                    return;

                case "reportIntervalMs":
                    builder.reportIntervalMs = Long.parseLong(value);
                    return;

                case "filterUpdateInitialDelayMs":
                    builder.filterUpdateInitialDelayMs = Long.parseLong(value);
                    return;

                case "filterUpdateIntervalMs":
                    builder.filterUpdateIntervalMs = Long.parseLong(value);
                    return;

                default:
                    if (key.startsWith("filter.")) {
                        final String[] parts = key.split("\\.", 3);
                        if (3 != parts.length) {
                            InitLog.loading("invalid filter. key: " + key);
                            return;
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

                        return;
                    }

                    InitLog.loading("unrecognised key: " + key);
            }
        });

        builder.filters = filters.values().stream().map(Filter.Builder::build).collect(Collectors.toList());

        return builder;
    }
}
