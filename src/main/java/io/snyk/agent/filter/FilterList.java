package io.snyk.agent.filter;

import io.snyk.agent.logic.Config;
import io.snyk.agent.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FilterList {
    public final List<Filter> filters;
    public final Instant generated;

    // @GuardedBy("this")
    private final Set<List<String>> bannedGavs = new HashSet<>();

    // @VisibleForTesting
    FilterList(List<Filter> filters, Instant generated) {
        this.filters = Collections.unmodifiableList(filters);
        this.generated = generated;
    }

    public static FilterList loadBuiltInFilters(Log log) {
        try {
            log.debug("loading built-in filters from bundled snapshot");
            return loadFiltersFrom(log,
                    loadResourceAsLines("/methods.bundled.properties"),
                    Instant.EPOCH);
        } catch (RuntimeException error) {
            log.warn("failed loading bundled snapshot, falling back to build-time snapshot");
            log.stackTrace(error);
        }

        return loadFiltersFrom(log,
                loadResourceAsLines("/methods.properties"),
                Instant.EPOCH);
    }

    public static FilterList loadFiltersFrom(Log log, Iterable<String> lines, Instant created) {
        final Map<String, Filter.Builder> filters = new HashMap<>();

        Config.parsePropertiesFile(lines).forEach((key, value) -> {
            if (!key.startsWith("filter.")) {
                log.warn("invalid filter key: " + key);
                return;
            }

            final String[] parts = key.split("\\.", 3);
            if (3 != parts.length) {
                log.warn("invalid filter. key: " + key);
                return;
            }

            final String filterName = parts[1];
            final String filterCommand = parts[2];

            final Filter.Builder filter = filters.computeIfAbsent(filterName, Filter.Builder::new);

            switch (filterCommand) {
                case "artifact":
                    filter.artifact = value;
                    break;
                case "version":
                    filter.version = value;
                    break;
                case "paths":
                    filter.addPathsFrom(value);
                    break;
                default:
                    log.warn("unrecognised filter command: " + key);
            }
        });

        if (filters.isEmpty()) {
            throw new IllegalStateException("filter files must not be empty");
        }

        return new FilterList(filters.values().stream()
                .map(Filter.Builder::build)
                .collect(Collectors.toList()),
                created);
    }

    public static FilterList empty() {
        return new FilterList(Collections.emptyList(), Instant.EPOCH);
    }

    private synchronized boolean isBanned(List<String> gav) {
        return bannedGavs.contains(gav);
    }

    private synchronized void ban(List<String> gav) {
        bannedGavs.add(gav);
    }

    public boolean shouldProcessClass(Log log, List<String> gavs, String className) {
        // if we don't know anything about this jar, then just filter on the class name
        if (gavs.isEmpty()) {
            return filters.stream().anyMatch(filter -> filter.testClassName(className));
        }

        if (isBanned(gavs)) {
            return false;
        }

        boolean classMatched = false;
        boolean gavMatch = false;

        for (final Filter filter : filters) {
            // if the filter doesn't have any artifact data,
            // match only on the class name
            if (!filter.artifact.isPresent()) {
                if (filter.testClassName(className)) {
                    classMatched = true;
                    break;
                } else {
                    // some filter is naughty, so we must always do all the work
                    gavMatch = true;
                    continue;
                }
            }

            final String wanted = filter.artifact.get() + ":";
            Stream<String> matching = gavs.stream()
                    .filter(gav -> gav.startsWith(wanted));

            if (filter.version.isPresent()) {
                final VersionFilter vf = filter.version.get();
                matching = matching.filter(gav -> {
                    // maven:$group:$artifact:$version
                    final String[] parts = gav.split(":", 4);
                    return vf.test(parts[3]);
                });
            }

            if (matching.findAny().isPresent()) {
                gavMatch = true;
                if (filter.testClassName(className)) {
                    classMatched = true;
                    break;
                }
            }
        }

        if (!gavMatch) {
            log.debug("archive has no matching filters: " + gavs);
            ban(gavs);
        }

        return classMatched;
    }

    static List<String> loadResourceAsLines(String resourceName) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(FilterList.class.getResourceAsStream(
                resourceName)))) {
            return reader.lines().collect(Collectors.toList());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }


}
