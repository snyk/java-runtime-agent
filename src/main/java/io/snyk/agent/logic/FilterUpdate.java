package io.snyk.agent.logic;

import io.snyk.agent.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class FilterUpdate implements Runnable {
    private final Log log;
    private final Config config;
    private final Runnable onAttempted;
    private final URL homeBaseSnapshots;

    public FilterUpdate(Log log, Config config, Runnable onAttempted) throws MalformedURLException {
        this.log = log;
        this.config = config;
        this.onAttempted = onAttempted;
        this.homeBaseSnapshots = config.homeBaseUrl.resolve("snapshot/" + config.projectId + "/java").toURL();
    }

    @Override
    public void run() {
        while (true) {
            try {
                tryFetch();
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

    private void tryFetch() throws IOException {
        final URLConnection conn = homeBaseSnapshots.openConnection();
        conn.setRequestProperty("Accept", "text/vnd.snyk.filters");
        conn.connect();

        if (conn instanceof HttpURLConnection) {
            final int code = ((HttpURLConnection) conn).getResponseCode();
            if (200 != code) {
                throw new IllegalStateException("unexpected response code: " + code);
            }
        }

        final List<String> lines;
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(),
                StandardCharsets.UTF_8))) {
            lines = reader.lines().collect(Collectors.toList());
        }

        log.info("filters updated");
        config.filters.set(Collections.unmodifiableList(Config.builderFromLines(lines).filters));
    }
}
