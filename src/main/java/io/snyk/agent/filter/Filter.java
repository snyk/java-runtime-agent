package io.snyk.agent.filter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Represents a filter block from the config file, which can filter on:
 * * the full class and name of a method
 * * the artifact and version of a jar, if known
 */
public class Filter {
    /**
     * user specified name for this filter, we don't care
     */
    public final String name;

    /**
     * the maven artifact in `org.apache.commons:commons-lang` format
     */
    public final Optional<String> artifact;
    public final Optional<VersionFilter> version;
    public final List<PathFilter> pathFilters;

    public final AtomicLong matches = new AtomicLong();

    public Filter(String name,
                  Optional<String> artifact,
                  Optional<VersionFilter> version,
                  List<PathFilter> pathFilters) {
        this.name = name;
        this.artifact = artifact;
        this.version = version;
        if (pathFilters.isEmpty()) {
            throw new IllegalStateException("filter error: " + name + ".paths must be provided");
        }
        this.pathFilters = pathFilters;
    }

    public static class Builder {
        private final String name;
        public String artifact;
        public String version;
        private final List<String> paths = new ArrayList<>();

        public Builder(String name) {
            this.name = name;
        }

        public void addPathsFrom(String value) {
            paths.addAll(Arrays.asList(value.split("\\s+")));
        }

        public Filter build() {
            if (null == artifact && null != version) {
                throw new IllegalStateException("filtering for version but not artifact doesn't make sense");
            }

            return new Filter(name,
                    Optional.ofNullable(artifact),
                    Optional.ofNullable(version).map(VersionFilter::parse),
                    paths.stream().map(PathFilter::parse).collect(Collectors.toList()));
        }
    }

    public boolean testClassName(String className) {
        return pathFilters.stream().anyMatch(filter -> filter.testClass(className));
    }

    public boolean testMethod(String className, String methodName) {
        return pathFilters.stream().anyMatch(filter -> filter.testMethod(className, methodName));
    }
}
