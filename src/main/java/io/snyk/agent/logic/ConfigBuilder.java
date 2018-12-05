package io.snyk.agent.logic;

import io.snyk.agent.filter.Filter;

import java.time.Instant;
import java.util.List;

/**
 * Helper builder for {@link Config}, only used internally.
 */
class ConfigBuilder {
    private static final int MINUTE_MS = 60_000;

    String projectId;
    List<Filter> filters;
    Instant providedFiltersGenerated = Instant.EPOCH;
    String homeBaseUrl;
    Long homeBasePostLimit;
    long startupDelayMs = 1_000;
    long heartBeatIntervalMs = MINUTE_MS / 2;
    long reportIntervalMs = MINUTE_MS;
    long filterUpdateIntervalMs = 60 * MINUTE_MS;
    long filterUpdateInitialDelayMs = 1_000;

    boolean trackClassLoading;
    boolean trackAccessors;
    boolean trackBranchingMethods;
    String logTo;
    boolean debugLoggingEnabled;
    boolean skipBuiltInRules;
    boolean skipMetaPosts;

    Config build() {
        return new Config(projectId, filters, providedFiltersGenerated, homeBaseUrl, homeBasePostLimit,
                startupDelayMs, heartBeatIntervalMs, reportIntervalMs,
                filterUpdateIntervalMs, filterUpdateInitialDelayMs,
                trackClassLoading, trackAccessors, trackBranchingMethods,
                logTo, debugLoggingEnabled, skipBuiltInRules, skipMetaPosts);
    }
}