package io.snyk.agent.logic;

import io.snyk.agent.filter.FilterList;
import io.snyk.agent.util.InitLog;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

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
    public final boolean skipBuiltInRules;
    public final boolean skipMetaPosts;

    private static final long DEFAULT_POST_LIMIT = 1024 * 1024;

    Config(String projectId,
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
        this.filters = new AtomicReference<>(FilterList.empty());
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

    public static Config loadConfigFromFile(String path) {
        try {
            InitLog.loading("loading config from: " + path);
            return loadConfig(Files.readAllLines(Paths.get(path)));
        } catch (IOException e) {
            InitLog.loading("error reading config file");
            throw new IllegalStateException(e);
        }
    }

    public static Map<String, String> parsePropertiesFile(Iterable<String> lines) {
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

    static Config loadConfig(Iterable<String> lines) {
        final ConfigBuilder builder = new ConfigBuilder();

        parsePropertiesFile(lines).forEach((key, value) -> {
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
                    InitLog.loading("unrecognised key: " + key);
            }
        });

        return builder.build();
    }
}
