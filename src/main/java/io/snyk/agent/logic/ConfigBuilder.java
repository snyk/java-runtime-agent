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
    boolean trackClassLoading;
    boolean trackAccessors;
    boolean trackBranchingMethods;
    boolean debugLoggingEnabled;

    Config build() throws MalformedURLException {
        return new Config(projectId, filters, homeBaseUrl, homeBasePostLimit,
                trackClassLoading, trackAccessors, trackBranchingMethods,
                debugLoggingEnabled);
    }
}