package io.snyk.agent.filter;

import io.snyk.agent.util.Log;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class FilterList {
    public final List<Filter> filters;

    // @GuardedBy("this")
    private final Set<List<String>> bannedGavs = new HashSet<>();

    public FilterList(List<Filter> filters) {
        this.filters = Collections.unmodifiableList(filters);
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
}
