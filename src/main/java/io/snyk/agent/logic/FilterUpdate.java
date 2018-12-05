package io.snyk.agent.logic;

import io.snyk.agent.filter.Filter;
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
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class FilterUpdate implements Runnable {
    private final Log log;
    private final Instrumentation instrumentation;
    private final Runnable onAttempted;
    private final URL homeBaseSnapshots;
    private final AtomicReference<List<Filter>> filters;
    private final long filterUpdateIntervalMs;

    private Instant dataFrom;

    public FilterUpdate(Log log,
                        Config config,
                        Instrumentation instrumentation,
                        Runnable onAttempted) throws MalformedURLException {
        this(log,
                instrumentation,
                onAttempted,
                config.homeBaseUrl.resolve("snapshot/" + config.projectId + "/java").toURL(),
                config.providedFiltersGenerated,
                config.filters,
                config.filterUpdateIntervalMs);
    }

    FilterUpdate(Log log,
                 Instrumentation instrumentation,
                 Runnable onAttempted,
                 URL homeBaseSnapshots,
                 Instant dataFrom,
                 AtomicReference<List<Filter>> filters,
                 long filterUpdateIntervalMs) {
        this.log = log;
        this.instrumentation = instrumentation;
        this.onAttempted = onAttempted;
        this.homeBaseSnapshots = homeBaseSnapshots;
        this.dataFrom = dataFrom;
        this.filters = filters;
        this.filterUpdateIntervalMs = filterUpdateIntervalMs;
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
                Thread.sleep(filterUpdateIntervalMs);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    private static String httpDateFormat(Instant date) {
        return DateTimeFormatter.RFC_1123_DATE_TIME.format(inUtc(date));
    }

    private static ZonedDateTime inUtc(Instant date) {
        return ZonedDateTime.ofInstant(date, ZoneOffset.UTC);
    }

    boolean fetchUpdatedAnything() throws IOException {
        final URLConnection conn = homeBaseSnapshots.openConnection();
        conn.setRequestProperty("Accept", "text/vnd.snyk.filters");
        conn.setRequestProperty("If-Modified-Since", httpDateFormat(dataFrom));
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

        final List<Filter> newFilters = Collections.unmodifiableList(Config.builderFromLines(lines).filters);
        filters.set(newFilters);
        dataFrom = Instant.ofEpochMilli(conn.getHeaderFieldDate("Last-Modified", 0));

        log.info("filters updated," +
                " new count: " + newFilters.size() +
                ", new date: " + DateTimeFormatter.ISO_DATE_TIME.format(inUtc(dataFrom)));

        return true;
    }

    private void reTransform() {
        for (Class someClass : instrumentation.getAllLoadedClasses()) {
            if (!instrumentation.isModifiableClass(someClass)) {
                continue;
            }

            final String candidateName = someClass.getName().replace('.', '/');
            if (filters.get().stream()
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
