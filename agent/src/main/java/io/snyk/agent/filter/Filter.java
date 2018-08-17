package io.snyk.agent.filter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Filter {
    /**
     * user specified name for this filter, we don't care
     */
    private final String name;

    /**
     * the maven artifact in `org.apache.commons:commons-lang` format
     */
    private final Optional<String> artifact;
    private final Optional<VersionFilter> version;
    private final List<PathFilter> pathFilters;

    public Filter(String name,
                  Optional<String> artifact,
                  Optional<VersionFilter> version,
                  List<PathFilter> pathFilters) {
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
            if (null == artifact && null != version) {
                throw new IllegalStateException("filtering for version but not artifact doesn't make sense");
            }

            return new Filter(name,
                    Optional.ofNullable(artifact),
                    Optional.ofNullable(version).map(VersionFilter::parse),
                    paths.stream().map(PathFilter::parse).collect(Collectors.toList()));
        }
    }

    public boolean test(List<String> artifacts, String path) {
        if (artifact.isPresent() && !artifacts.isEmpty()) {
            // if we have any artifacts mentioned in the jar,
            // then one of them must be our filter
            final String artifact = this.artifact.get() + ":";
            Stream<String> matching = artifacts.stream().filter(a -> a.startsWith(artifact));

            if (version.isPresent()) {
                final VersionFilter vf = version.get();
                matching = matching.filter(art -> {
                    final String[] parts = art.split(":", 3);
                    return vf.test(parts[2]);
                });
            }

            if (!matching.findAny().isPresent()) {
                return false;
            }
        }

        if (pathFilters.isEmpty()) {
            return true;
        }

        return pathFilters.stream().anyMatch(filter -> filter.test(path));
    }
}
