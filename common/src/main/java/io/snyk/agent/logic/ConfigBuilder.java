package io.snyk.agent.logic;

import io.snyk.agent.filter.Filter;

import java.util.List;

class ConfigBuilder {
    String projectId;
    List<Filter> filters;
    String urlPrefix;
    boolean trackClassLoading;
    boolean trackAccessors;
    boolean trackBranchingMethods;
    boolean debugLoggingEnabled;

    Config build() {
        return new Config(projectId, filters, urlPrefix, trackClassLoading,
                trackAccessors,
                trackBranchingMethods, debugLoggingEnabled);
    }
}