package io.snyk.agent.filter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Filter {
    private final String name;
    private final String artifact;
    private final String version;
    private final List<PathFilter> pathFilters;

    public Filter(String name, String artifact, String version, List<PathFilter> pathFilters) {
        this.name = name;
        this.artifact = artifact;
        this.version = version;
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
            return new Filter(name,
                    artifact,
                    version,
                    paths.stream().map(PathFilter::parse).collect(Collectors.toList()));
        }
    }
}
