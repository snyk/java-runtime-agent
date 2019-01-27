package io.snyk.agent.logic;

import io.snyk.agent.filter.Filter;
import io.snyk.agent.filter.FilterList;
import io.snyk.agent.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.instrument.Instrumentation;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

public class FilterUpdate implements Runnable {
    private final Log log;
    private final Config config;
    private final Instrumentation instrumentation;
    private final Runnable onAttempted;
    private final URL homeBaseSnapshots;

    public FilterUpdate(Log log,
                        Config config,
                        Instrumentation instrumentation,
                        Runnable onAttempted) throws MalformedURLException {
        this.log = log;
        this.config = config;
        this.instrumentation = instrumentation;
        this.onAttempted = onAttempted;
        this.homeBaseSnapshots = config.homeBaseUrl.resolve("v2/snapshot/" + config.projectId + "/java").toURL();
    }

    @Override
    public void run() {
        while (true) {
            try {
                if (fetchUpdatedAnything()) {
                    reTransform();
                }
            } catch (Throwable t) {
                log.warn("agent update issue");
                log.stackTrace(t);
            }

            onAttempted.run();

            try {
                Thread.sleep(config.filterUpdateIntervalMs);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    private boolean fetchUpdatedAnything() throws IOException {
        final URLConnection conn = homeBaseSnapshots.openConnection();
        conn.setRequestProperty("Accept", "text/vnd.snyk.filters");
        conn.connect();

        if (conn instanceof HttpURLConnection) {
            final int code = ((HttpURLConnection) conn).getResponseCode();
            if (304 == code) {
                log.debug("no newer filters available");
                return false;
            }

            if (200 != code) {
                throw new IllegalStateException("unexpected response code: " + code);
            }
        }

        final List<String> lines;
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(),
                StandardCharsets.UTF_8))) {
            lines = reader.lines().collect(Collectors.toList());
        }

        config.filters.set(FilterList.loadFiltersFrom(log, lines));
        log.info("filters updated, new count: " + config.filters.get().filters.size());

        return true;
    }

    private void reTransform() {
        final List<Filter> filters = config.filters.get().filters;

        for (Class someClass : instrumentation.getAllLoadedClasses()) {
            if (!instrumentation.isModifiableClass(someClass)) {
                continue;
            }

            final String candidateName = someClass.getName().replace('.', '/');
            if (filters.stream()
                    .noneMatch(f -> f.testClassName(candidateName))) {
                continue;
            }

            reTransformClass(someClass);
        }
    }

    private void reTransformClass(Class someClass) {
        try {
            instrumentation.retransformClasses(someClass);
        } catch (Exception e) {
            log.warn("retransforming failed: " + someClass.getName());
            log.stackTrace(e);
        }
    }
}
