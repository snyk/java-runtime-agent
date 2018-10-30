package io.snyk.agent.logic;

import io.snyk.agent.filter.Filter;

import java.net.MalformedURLException;
import java.util.List;

/**
 * Helper builder for {@link Config}, only used internally.
 */
class ConfigBuilder {
    String projectId;
    List<Filter> filters;
    String homeBaseUrl;
    Long homeBasePostLimit;
    long startupDelayMs = 1_000;
    long heartBeatIntervalMs = 30_000;
    long reportIntervalMs = 60_000;
    boolean trackClassLoading;
    boolean trackAccessors;
    boolean trackBranchingMethods;
    boolean debugLoggingEnabled;
    boolean skipBuiltInRules;
    boolean skipMetaPosts;

    Config build() {
        return new Config(projectId, filters, homeBaseUrl, homeBasePostLimit,
                startupDelayMs, heartBeatIntervalMs, reportIntervalMs,
                trackClassLoading, trackAccessors, trackBranchingMethods,
                debugLoggingEnabled, skipBuiltInRules, skipMetaPosts);
    }
}