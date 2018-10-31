package io.snyk.agent.logic;

import io.snyk.agent.jvm.LandingZone;
import io.snyk.agent.util.Json;
import io.snyk.agent.util.Log;
import io.snyk.agent.util.UseCounter;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * Sit around in the background, and occasionally send off beacons.
 */
public class ReportingWorker implements Runnable {
    private final String vmName;
    private final String vmVendor;
    private final String vmVersion;
    private final String hostName = computeHostName();
    private final UUID agentId = UUID.randomUUID();

    private final Log log;
    private final Config config;
    private final DataTracker dataTracker;
    private final Poster poster;

    private long lastSuccessfulBeacon = 0;

    {
        final RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        vmName = runtime.getName();
        vmVendor = runtime.getVmVendor();
        vmVersion = runtime.getVmVersion();
    }

    public ReportingWorker(Log log, Config config, DataTracker dataTracker) throws MalformedURLException {
        this.log = log;
        this.config = config;
        this.dataTracker = dataTracker;

        log.info("detected vmVendor: " + vmVendor);
        switch (config.homeBaseUrl.getScheme()) {
            case "file":
                poster = new DirectoryWritingPoster(config.homeBaseUrl);
                break;
            default:
                poster = new StdLibHttpPoster(config.homeBaseUrl.toURL());
                break;
        }
    }

    @Override
    public void run() {
        try {
            // arbitrary: no point running during early vm startup;
            // no harm, but no useful information?
            Thread.sleep(config.startupDelayMs);
        } catch (InterruptedException e) {
            return;
        }
        while (true) {
            try {
                work(LandingZone.SEEN_SET.drain());
            } catch (Throwable t) {
                log.warn("agent issue");
                log.stackTrace(t);
                dataTracker.addError("agent-send", t);
            }
            try {
                Thread.sleep(config.heartBeatIntervalMs);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    // @VisibleForTesting
    void work(UseCounter.Drain drain) {
        try {
            doPosting(drain, this.poster);
        } catch (Exception e) {
            log.warn("reporting failed");
            log.stackTrace(e);
        }
    }

    void doPosting(UseCounter.Drain from, Poster poster)
            throws IOException {
        final byte[] prefix = jsonHeader().toString().getBytes(StandardCharsets.UTF_8);

        if (!config.skipMetaPosts) {
            poster.sendFragment(prefix, "\"heartbeat\": true");
        }

        final long now = System.currentTimeMillis();

        // if we've reported recently enough; abs() so insane clock drift won't break us forever
        if (Math.abs(now - this.lastSuccessfulBeacon) < config.reportIntervalMs) {
            return;
        }

        if (!config.skipMetaPosts) {
            poster.sendFragment(prefix, buildMeta());
        }

        postArray(poster, prefix, "eventsToSend", from.methodEntries.iterator(), this::appendMethodEntry);
        postArray(poster, prefix, "eventsToSend", from.loadClasses.entrySet().iterator(), this::appendLoadClass);
        postArray(poster, prefix, "errors", dataTracker.errors.iterator(), this::appendError);

        // intentionally at the end; this method might terminate by exception
        this.lastSuccessfulBeacon = now;
    }

    private <T> void postArray(Poster poster,
                               byte[] prefix,
                               String fieldName,
                               Iterator<T> array,
                               BiConsumer<StringBuilder, T> appender)
            throws IOException {
        int fragmentNumber = 0;
        while (array.hasNext()) {
            final StringBuilder msg = new StringBuilder(4096);
            msg.append("\"").append(fieldName).append("\":[\n");

            // guarantee progress by always sending at least one event
            appender.accept(msg, array.next());

            while (array.hasNext() && msg.length() < config.homeBasePostLimit) {
                appender.accept(msg, array.next());
            }

            trimRightCommaSpacing(msg);
            msg.append("]");

            poster.sendFragment(prefix, msg.toString());

            fragmentNumber++;

            log.info(fieldName + ": sent part " + fragmentNumber + ", chars: " + msg.length());
        }
    }

    CharSequence jsonHeader() {
        final StringBuilder msg = new StringBuilder(4096);
        msg.append("{\"projectId\":");
        Json.appendString(msg, config.projectId);
        msg.append(", \"timestamp\":");
        Json.appendString(msg, Instant.now().toString());

        msg.append(", \"systemInfo\":{\n");
        msg.append("   \"hostName\":");
        Json.appendString(msg, hostName);
        msg.append(",  \"jvm\":{");
        msg.append("\"vmName\":");
        Json.appendString(msg, vmName);
        msg.append(",\"vendor\":");
        Json.appendString(msg, vmVendor);
        msg.append(",\"version\":");
        Json.appendString(msg, vmVersion);
        msg.append("}},\"correlationId\":");
        Json.appendString(msg, UUID.randomUUID().toString());
        msg.append(",\"agentId\":");
        Json.appendString(msg, agentId.toString());
        msg.append(",");
        return msg;
    }

    private void appendMethodEntry(StringBuilder msg, String rawLocator) {
        final String[] parts = rawLocator.split(":", 4);
        final String className = parts[0];
        final String methodName = parts[1];
        final String classCrc32c = parts[2];
        final String sourceUrl = parts[3];
        final URI sourceUri = URI.create(sourceUrl);
        msg.append("{\"methodEntry\":{");
        msg.append("\"source\":\"java-agent\",");
        msg.append("\"coordinates\":[");
        for (String jarInfo : dataTracker.classInfo.infoFor(sourceUri)
                .stream().sorted().collect(Collectors.toList())) {
            Json.appendString(msg, jarInfo);
            msg.append(",");
        }
        trimRightCommaSpacing(msg);
        msg.append("],");
        msg.append("\"methodName\":");
        Json.appendString(msg, className + "#" + methodName);
        msg.append(",\"sourceUri\":");
        Json.appendString(msg, sourceUrl);
        msg.append(",\"sourceCrc32c\":");
        Json.appendString(msg, classCrc32c);
        msg.append("}},\n");
    }

    private void appendLoadClass(StringBuilder msg, Map.Entry<String, HashSet<String>> entry) {
        final String caller = entry.getKey();
        final HashSet<String> loaded = entry.getValue();

        msg.append("{\"loadClass\":{");
        msg.append("\"from\":");
        Json.appendString(msg, caller);
        msg.append(",\"args\":[");
        loaded.forEach(arg -> {
            msg.append("\n  ");
            Json.appendString(msg, arg);
            msg.append(",");
        });
        trimRightCommaSpacing(msg);
        msg.append("]}},");
    }

    private void appendError(StringBuilder msg, ObservedError error) {
        msg.append("{\"msg\":");
        Json.appendString(msg, error.msg);
        msg.append(",\"exception\":");
        Json.appendString(msg, error.problem);
        msg.append("},");
    }

    private StringBuilder buildMeta() {
        final StringBuilder msg = new StringBuilder(1024);
        msg.append("\"filters\":[\n");
        config.filters.forEach(filter -> {
            msg.append("{\"name\":");
            Json.appendString(msg, filter.name);
            filter.artifact.ifPresent(artifact -> {
                msg.append(",\"artifact\":");
                Json.appendString(msg, artifact);
            });
            msg.append(",\"paths\":[");
            filter.pathFilters.forEach(pathFilter -> {
                msg.append("{\"className\":");
                Json.appendString(msg, pathFilter.className);
                pathFilter.methodName.ifPresent(methodName -> {
                    msg.append(",\"methodName\":");
                    Json.appendString(msg, methodName);
                });
                msg.append(",\"classNameIsPrefix\":");
                msg.append(pathFilter.classNameIsPrefix);
                msg.append("},");
            });
            trimRightCommaSpacing(msg);
            msg.append("]},");
        });
        trimRightCommaSpacing(msg);
        msg.append("],\"loadedSources\":{\n");

        dataTracker.classInfo.all()
                .entrySet().stream().sorted(Comparator.comparing(Map.Entry::getKey)).forEachOrdered(entry -> {
            Json.appendString(msg, entry.getKey().toString());
            msg.append(":[");
            entry.getValue().stream().sorted().forEachOrdered(locator -> {
                Json.appendString(msg, locator);
                msg.append(",");
            });
            trimRightCommaSpacing(msg);
            msg.append("],\n");
        });
        trimRightCommaSpacing(msg);
        msg.append("}\n");

        return msg;
    }

    static void trimRightCommaSpacing(StringBuilder msg) {
        if (0 == msg.length()) {
            return;
        }

        while (0 != msg.length()) {
            final char last = msg.charAt(msg.length() - 1);
            if (Character.isWhitespace(last) || ',' == last) {
                msg.setLength(msg.length() - 1);
            } else {
                break;
            }
        }
    }

    private static String computeHostName() {
        for (String env : new String[]{"SNYK_MACHINE_NAME", "HOSTNAME", "COMPUTERNAME"}) {
            final String val = System.getenv(env);
            if (null != val) {
                final String trim = val.trim();
                if (!trim.isEmpty()) {
                    return trim;
                }
            }
        }

        try {
            final Optional<String> line = Files.lines(new File("/etc/hostname").toPath()).findFirst();
            if (line.isPresent()) {
                final String trim = line.get().trim();
                if (!trim.isEmpty()) {
                    return trim;
                }
            }
        } catch (IOException ignored) {
            // no such file, couldn't read it, etc.
            // e.g. docker, wrong OS (Windows)
        }

        try {
            final String line = runHostname();
            if (null != line) {
                final String trim = line.trim();
                if (!trim.isEmpty()) {
                    return trim;
                }
            }
        } catch (IOException ignored) {
            // no such command, no permission to execute it, etc.
        }

        return "unknown";
    }

    private static String runHostname() throws IOException {
        final Process proc = new ProcessBuilder("hostname").start();
        proc.getOutputStream().close();
        proc.getErrorStream().close();
        try (final InputStream input = proc.getInputStream();
             final InputStreamReader reader = new InputStreamReader(input);
             final BufferedReader buffered = new BufferedReader(reader)) {
            return buffered.readLine();
        }
    }

    /**
     * Try a bit harder to read bytes from a reader, even if it blocks for a bit.
     */
    private static int readMany(final InputStream input, final byte[] buffer) throws IOException {
        int remaining = buffer.length;
        while (remaining > 0) {
            final int location = buffer.length - remaining;
            final int count = input.read(buffer, location, remaining);
            // -1 means EOF
            if (-1 == count) {
                break;
            }
            remaining -= count;
        }
        return buffer.length - remaining;
    }

    interface Poster {
        void sendFragment(byte[] prefix, CharSequence msg) throws IOException;
    }

    class StdLibHttpPoster implements Poster, AutoCloseable {

        private final URL destination;
        private HttpURLConnection lastConnection = null;

        StdLibHttpPoster(URL destination) {
            this.destination = destination;
        }

        @Override
        public void sendFragment(byte[] prefix, CharSequence msg) throws IOException {
            final byte[] middle = msg.toString().getBytes(StandardCharsets.UTF_8);
            final int totalLen = prefix.length + middle.length + 1 /* closing brace */;

            final HttpURLConnection conn = (HttpURLConnection)
                    destination.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setFixedLengthStreamingMode(totalLen);
            conn.setDoOutput(true);
            try (final OutputStream body = conn.getOutputStream()) {
                body.write(prefix);
                body.write(middle);
                body.write('}');
            }
            try (final InputStream response = conn.getInputStream()) {
                final byte[] buffer = new byte[16 * 1024];
                final int count = readMany(response, buffer);
                final String reply = new String(buffer, 0, count, StandardCharsets.UTF_8);
                log.info("reply: " + reply);
            }
        }


        @Override
        public void close() throws Exception {
            if (null != lastConnection) {
                lastConnection.disconnect();
                lastConnection = null;
            }
        }
    }

    class DirectoryWritingPoster implements Poster {

        private final File root;
        long messageNumber = 0;

        DirectoryWritingPoster(URI homeBaseUrl) {
            root = new File(homeBaseUrl.getPath());
            if (!root.isDirectory() && !root.mkdirs()) {
                throw new IllegalStateException("couldn't create output directory: " + root);
            }
        }

        @Override
        public void sendFragment(byte[] prefix, CharSequence msg) throws IOException {
            final String fileName = "post-" + messageNumber + ".json";
            try (final FileOutputStream fos = new FileOutputStream(new File(root, fileName))) {
                fos.write(prefix);
                fos.write(msg.toString().getBytes(StandardCharsets.UTF_8));
                fos.write('}');
            }
            messageNumber++;
        }
    }
}
