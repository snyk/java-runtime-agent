package io.snyk.agent.logic;

import io.snyk.agent.jvm.LandingZone;
import io.snyk.agent.util.Json;
import io.snyk.agent.util.UseCounter;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Optional;

public class ReportingWorker implements Runnable {
    private final String vmName = ManagementFactory.getRuntimeMXBean().getName();
    private final String hostName = computeHostName();
    private final Config config;
    private final ClassSource classSource;

    public ReportingWorker(Config config, ClassSource classSource) {
        this.config = config;
        this.classSource = classSource;
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
                System.err.println("agent issue");
                t.printStackTrace();
            }
            try {
                Thread.sleep(4000);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    void work(UseCounter.Drain drain) {
        final CharSequence msg = serialiseState(drain);

        try {
            final byte[] bytes = msg.toString().getBytes(StandardCharsets.UTF_8);

            final HttpURLConnection conn = (HttpURLConnection)
                    new URL(config.urlPrefix + "/api/v1/beacon").openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setFixedLengthStreamingMode(bytes.length);
            conn.setDoOutput(true);
            try (final OutputStream body = conn.getOutputStream()) {
                body.write(bytes);
            }
            conn.getInputStream().close();
            conn.disconnect();
        } catch (IOException e) {
            System.err.println("snyk explainer");
            e.printStackTrace();
        }
    }

    String serialiseState(UseCounter.Drain drain) {
        final StringBuilder msg = new StringBuilder(4096);
        msg.append("{\"projectId\":");
        Json.appendString(msg, config.projectId);
        msg.append(", \"hostName\":");
        Json.appendString(msg, hostName);
        msg.append(", \"vmName\":");
        Json.appendString(msg, vmName);
        msg.append(", \"eventsToSend\":[\n");

        for (String loc : drain.methodEntries) {
            msg.append("{\"info\":{");
            msg.append("\"methodName\":");
            Json.appendString(msg, loc);
            msg.append(",\"moduleInfo\":{\"java\": true}");
            msg.append("}},\n");
        }

        drain.loadClasses.forEach((caller, loaded) -> {
            msg.append("{\"info\":{");
            msg.append("\"methodName\":");
            Json.appendString(msg, caller);
            msg.append(",\"moduleInfo\":{\"java\": true}");
            msg.append(",\"args\":[");
            loaded.forEach(arg -> {
                msg.append("\n  ");
                Json.appendString(msg, arg);
                msg.append(",");
            });
            trimRightCommaSpacing(msg);
            msg.append("]}},");
        });

        trimRightCommaSpacing(msg);
        msg.append("\n]}");
        return msg.toString();
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
}
