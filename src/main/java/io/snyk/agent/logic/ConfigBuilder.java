package io.snyk.agent.logic;

/**
 * Helper builder for {@link Config}, only used internally.
 */
class ConfigBuilder {
    private static final int MINUTE_MS = 60_000;

    String projectId;
    String homeBaseUrl;
    Long homeBasePostLimit;
    boolean allowUnknownCA = false;
    long startupDelayMs = 1_000;
    long heartBeatIntervalMs = MINUTE_MS / 2;
    long reportIntervalMs = MINUTE_MS;
    long filterUpdateIntervalMs = 60 * MINUTE_MS;
    long filterUpdateInitialDelayMs = 1_000;
    boolean addShutdownHook = true;

    boolean trackClassLoading;
    boolean trackAccessors;
    boolean trackBranchingMethods;
    String logTo;
    boolean debugLoggingEnabled;
    boolean skipBuiltInRules;
    boolean skipMetaPosts;

    Config build() {
        return new Config(projectId, homeBaseUrl, homeBasePostLimit,
                allowUnknownCA, startupDelayMs, heartBeatIntervalMs, reportIntervalMs,
                filterUpdateIntervalMs, filterUpdateInitialDelayMs,
                trackClassLoading, trackAccessors, trackBranchingMethods,
                logTo, debugLoggingEnabled, skipBuiltInRules, skipMetaPosts,
                addShutdownHook);
    }
}
