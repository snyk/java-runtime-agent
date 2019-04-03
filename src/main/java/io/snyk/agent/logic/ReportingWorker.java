package io.snyk.agent.logic;

import io.snyk.agent.jvm.LandingZone;
import io.snyk.agent.jvm.MachineInfo;
import io.snyk.agent.jvm.Version;
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
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Sit around in the background, and occasionally send off beacons.
 */
public class ReportingWorker implements Runnable {
    private static final int NEVER_SENT_A_BEACON = 0;

    private MachineInfo info = null;
    private final UUID agentId = UUID.randomUUID();
    private final String agentVersion = Version.extendedVersionInfo();

    private final Log log;
    private final Config config;
    private final DataTracker dataTracker;
    private final Poster poster;

    private long lastSuccessfulBeacon = NEVER_SENT_A_BEACON;

    public ReportingWorker(Log log, Config config, DataTracker dataTracker) {
        this(log, config, dataTracker, computePoster(log, config));
    }

    // @VisibleForTesting
    ReportingWorker(Log log, Config config, DataTracker dataTracker, Poster poster) {
        this.log = log;
        this.config = config;
        this.dataTracker = dataTracker;
        this.poster = poster;

        // gathering the machine info can do DNS queries, which is inconvenient
        final Thread infoLoader = new Thread(() -> info = new MachineInfo());
        infoLoader.setDaemon(true);
        infoLoader.setName("snyk-agent");
        infoLoader.start();
    }

    private static Poster computePoster(Log log, Config config) {
        switch (config.homeBaseUrl.getScheme()) {
            case "file":
                return new DirectoryWritingPoster(config.homeBaseUrl);
            default:
                try {
                    return new StdLibHttpPoster(log, config.homeBaseUrl.resolve("v1/beacon").toURL());
                } catch (MalformedURLException e) {
                    throw new IllegalStateException("invalid beacon url", e);
                }
        }
    }

    @Override
    public void run() {
        log.info("reporting ready, but resting");

        try {
            // arbitrary: no point running during early vm startup;
            // no harm, but no useful information?
            Thread.sleep(config.startupDelayMs);
        } catch (InterruptedException e) {
            return;
        }

        setShutdownHook();

        log.info("reporting started");

        while (true) {
            try {
                sendIfNecessary(LandingZone.SEEN_SET::drain);
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

    /**
     * @param drainMaker Where to drain events from. Will not be called if it's only time for heartbeats.
     */
    void sendIfNecessary(Supplier<UseCounter.Drain> drainMaker) throws IOException {
        final byte[] prefix = jsonHeader().toString().getBytes(StandardCharsets.UTF_8);

        if (!config.skipMetaPosts) {
            poster.sendFragment(prefix, "\"heartbeat\": true");
        }

        final long now = System.currentTimeMillis();

        // if we've reported recently enough; abs() so insane clock drift won't break us forever
        if (Math.abs(now - this.lastSuccessfulBeacon) < config.reportIntervalMs) {
            return;
        }

        send(drainMaker, prefix);

        if (NEVER_SENT_A_BEACON == this.lastSuccessfulBeacon) {
            log.info("successfully transmitted our first beacon");
        }

        // intentionally at the end; this method might terminate by exception
        this.lastSuccessfulBeacon = now;
    }

     /**
     * @param drainMaker Where to drain events from. Will not be called if it's only time for heartbeats.
     * @param prefix each array of events posted will be prefixed with this byte[] encoded string
     */
    synchronized void send(Supplier<UseCounter.Drain> drainMaker, byte[] prefix) throws IOException {
        if (!config.skipMetaPosts) {
            poster.sendFragment(prefix, buildMeta());
        }

        final UseCounter.Drain from = drainMaker.get();
        postArray(poster, prefix, "eventsToSend", from.methodEntries.iterator(), this::appendMethodEntry);
        postArray(poster, prefix, "eventsToSend", from.loadClasses.entrySet().iterator(), this::appendLoadClass);
        postArray(poster, prefix, "errors", dataTracker.errors.iterator(), this::appendError);
    }

    private void setShutdownHook() {
        try {
            log.info("attempting to set up a shutdown hook that drains events one last time");
            final Thread shutdownHook = new Thread(() -> safeSend(LandingZone.SEEN_SET::drain));
            Runtime.getRuntime().addShutdownHook(shutdownHook);
            log.info("shutdown hook set up successfully");
        } catch (IllegalArgumentException e) {
            // If the specified hook has already been registered,
            // or if it can be determined that the hook is already running or has already been run.
            // so either way we're set?
        } catch (IllegalStateException e) {
            // If the virtual machine is already in the process of shutting down
            // this means we're okay with not setting up that hook
        } catch (SecurityException e) {
            // If a security manager is present and it denies RuntimePermission("shutdownHooks")
            // not much to do about this one except log?
            log.warn("failed to set a shutdownHook due to denied permission");
        }
    }

    private void safeSend(Supplier<UseCounter.Drain> drainMaker) {
        try {
            final byte[] prefix = jsonHeader().toString().getBytes(StandardCharsets.UTF_8);
            send(drainMaker, prefix);
        } catch (Exception ignored) {
            // intentionally disregard errors to allow fast shut down
        }
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

            log.debug(fieldName + ": sent part " + fragmentNumber + ", chars: " + msg.length());
        }
    }

    private CharSequence jsonHeader() {
        final StringBuilder msg = new StringBuilder(4096);
        msg.append("{\"projectId\":");
        Json.appendString(msg, config.projectId);
        msg.append(", \"timestamp\":");
        Json.appendString(msg, Instant.now().toString());

        msg.append(", \"systemInfo\":{\n");
        msg.append("   \"agentVersion\":");
        Json.appendString(msg, agentVersion);
        msg.append(",  \"hostName\":");
        Json.appendString(msg, null != info ? info.hostName : "n/a");
        msg.append(",  \"jvm\":{");
        msg.append("\"vmName\":");
        Json.appendString(msg, null != info ? info.vmName : "n/a");
        msg.append(",\"vendor\":");
        Json.appendString(msg, null != info ? info.vmVendor : "n/a");
        msg.append(",\"version\":");
        Json.appendString(msg, null != info ? info.vmVersion : "n/a");
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
        msg.append("},\"timestamp\":");
        Json.appendString(msg, DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
        msg.append("},\n");
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
        msg.append("\"loadedSources\":{\n");

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

    static class StdLibHttpPoster implements Poster, AutoCloseable {

        private final Log log;
        private final URL destination;
        private HttpURLConnection lastConnection = null;

        StdLibHttpPoster(Log log, URL destination) {
            this.log = log;
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
                log.debug("reply: " + reply);
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

    static class DirectoryWritingPoster implements Poster {

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
