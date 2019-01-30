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

    public List<Filter> applicableFilters(Log log, List<String> gavs, String className) {
        if (gavs.isEmpty()) {
            // we don't know about the jar, so have to process it
            return filters.stream()
                    .filter(filter -> filter.testClassName(className))
                    .collect(Collectors.toList());
        }

        // if we've already seen that the gav can't match, so it still won't match
        if (isBanned(gavs)) {
            return Collections.emptyList();
        }

        final List<Filter> matchingFilters = this.filters.stream().filter(filter -> {
            if (!filter.artifact.isPresent()) {
                return true;
            }

            final String wanted = filter.artifact.get() + ":";
            final Stream<String> matchingGavs = gavs.stream()
                    .filter(gav -> gav.startsWith(wanted));

            if (!filter.version.isPresent()) {
                return matchingGavs.findAny().isPresent();
            }

            final VersionFilter vf = filter.version.get();
            return matchingGavs.anyMatch(gav -> {
                // maven:$group:$artifact:$version
                final String[] parts = gav.split(":", 4);
                return vf.test(parts[3]);
            });
        }).collect(Collectors.toList());

        if (matchingFilters.isEmpty()) {
            log.debug("archive has no matching filters: " + gavs);
            ban(gavs);
        }

        return matchingFilters.stream()
                .filter(filter -> filter.testClassName(className))
                .collect(Collectors.toList());
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
