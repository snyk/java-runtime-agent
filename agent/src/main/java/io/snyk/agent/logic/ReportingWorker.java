package io.snyk.agent.logic;

import io.snyk.agent.jvm.LandingZone;
import io.snyk.agent.util.Json;
import io.snyk.agent.util.Log;
import io.snyk.agent.util.UseCounter;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class ReportingWorker implements Runnable {
    private final String vmName;
    private final String vmVendor;
    private final String vmVersion;
    private final String hostName = computeHostName();
    private final Log log;
    private final Config config;
    private final ClassSource classSource;

    {
        final RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        vmName = runtime.getName();
        vmVendor = runtime.getVmVendor();
        vmVersion = runtime.getVmVersion();
    }

    public ReportingWorker(Log log, Config config, ClassSource classSource) {
        this.log = log;
        this.config = config;
        this.classSource = classSource;

        log.info("detected vmVendor: " + vmVendor);
    }

    @Override
    public void run() {
        try {
            // arbitrary: no point running during early vm startup;
            // no harm, but no useful information?
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            return;
        }
        while (true) {
            try {
                work(LandingZone.SEEN_SET.drain());
            } catch (Throwable t) {
                log.warn("agent issue");
                log.stackTrace(t);
                classSource.addError("agent-send", t);
            }
            try {
                Thread.sleep(4000);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    private void work(UseCounter.Drain drain) {
        try (final StdLibHttpPoster homeBaseEndpoint = new StdLibHttpPoster(jsonHeader().toString(), new URL(config.homeBaseUrl))) {
            doPosting(drain, homeBaseEndpoint);
        } catch (Exception e) {
            log.warn("reporting failed");
            log.stackTrace(e);
        }
    }

    void doPosting(UseCounter.Drain from, Poster poster)
            throws IOException {
        poster.sendFragment(buildMeta());
        postArray(poster, "eventsToSend", from.methodEntries.iterator(), this::appendMethodEntry);
        postArray(poster, "eventsToSend", from.loadClasses.entrySet().iterator(), this::appendLoadClass);
        postArray(poster, "errors", classSource.errors.iterator(), this::appendError);
    }

    private <T> void postArray(Poster poster,
                               String fieldName,
                               Iterator<T> array,
                               BiConsumer<StringBuilder, T> appender)
            throws IOException {
        while (array.hasNext()) {
            final StringBuilder msg = new StringBuilder(4096);
            msg.append("\"" + fieldName + "\":[\n");

            // guarantee progress by always sending at least one event
            appender.accept(msg, array.next());

            while (array.hasNext() && msg.length() < config.homeBasePostLimit) {
                appender.accept(msg, array.next());
            }

            trimRightCommaSpacing(msg);
            msg.append("]");

            poster.sendFragment(msg.toString());
        }
    }

    private CharSequence jsonHeader() {
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
        msg.append(",\"correlationId\":");
        Json.appendString(msg, UUID.randomUUID().toString());
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
        msg.append("\"className\":");
        Json.appendString(msg, className);
        msg.append(",\"methodName\":");
        Json.appendString(msg, methodName);
        msg.append(",\"classCrc32c\":");
        Json.appendString(msg, classCrc32c);
        msg.append(",\"sourceUri\":");
        Json.appendString(msg, sourceUrl);
        msg.append(",\"jarInfo\":[");
        for (String jarInfo : classSource.infoFor(sourceUri)
                .stream().sorted().collect(Collectors.toList())) {
            Json.appendString(msg, jarInfo);
            msg.append(",");
        }
        trimRightCommaSpacing(msg);
        msg.append("]}},\n");
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
            filter.version.ifPresent(version -> {
                msg.append(",\"version\":");
                Json.appendString(msg, version.version.toString());

                // TODO: this isn't great, in future this will allow other things...
                msg.append(",\"versionDirection\":");
                msg.append(version.direction);
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

        classSource.all().entrySet().stream().sorted(Comparator.comparing(Map.Entry::getKey)).forEachOrdered(entry -> {
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
        void sendFragment(CharSequence msg) throws IOException;
    }

    class StdLibHttpPoster implements Poster, AutoCloseable {

        private final String prefix;
        private final URL destination;
        private HttpURLConnection lastConnection = null;

        StdLibHttpPoster(String prefix, URL destination) {
            this.prefix = prefix;
            this.destination = destination;
        }

        @Override
        public void sendFragment(CharSequence msg) throws IOException {
            final byte[] bytes = fullMessage(msg);

            final HttpURLConnection conn = (HttpURLConnection)
                    destination.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setFixedLengthStreamingMode(bytes.length);
            conn.setDoOutput(true);
            try (final OutputStream body = conn.getOutputStream()) {
                body.write(bytes);
            }
            try (final InputStream response = conn.getInputStream()) {
                final byte[] buffer = new byte[16 * 1024];
                final int count = readMany(response, buffer);
                final String reply = new String(buffer, 0, count, StandardCharsets.UTF_8);
                log.info("reply: " + reply);
            }
        }

        private byte[] fullMessage(CharSequence msg) {
            final StringBuilder wholeMessage = new StringBuilder(prefix.length() + msg.length() + 32);
            wholeMessage.append(prefix);
            wholeMessage.append(msg);
            wholeMessage.append("}");

            return wholeMessage.toString().getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public void close() throws Exception {
            if (null != lastConnection) {
                lastConnection.disconnect();
                lastConnection = null;
            }
        }
    }
}
