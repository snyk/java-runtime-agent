package io.snyk.agent.logic;

import io.snyk.agent.filter.Filter;

import java.util.List;

class ConfigBuilder {
    String projectId;
    List<Filter> filters;
    String homeBaseUrl;
    Long homeBasePostLimit;
    boolean trackClassLoading;
    boolean trackAccessors;
    boolean trackBranchingMethods;
    boolean debugLoggingEnabled;

    Config build() {
        return new Config(projectId, filters, homeBaseUrl, homeBasePostLimit,
                trackClassLoading, trackAccessors, trackBranchingMethods,
                debugLoggingEnabled);
    }
}