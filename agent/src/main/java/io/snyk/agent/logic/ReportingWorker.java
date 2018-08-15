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
                work();
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

    private void work() {
        final StringBuilder msg = new StringBuilder(4096);
        msg.append("{\"projectId\":");
        Json.appendString(msg, hostName + "//" + vmName);
        msg.append(", \"hostName\":");
        Json.appendString(msg, hostName);
        msg.append(", \"vmName\":");
        Json.appendString(msg, vmName);
        msg.append(", \"eventsToSend\":[\n");

        final UseCounter.Drain drain = LandingZone.SEEN_SET.drain();
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
            msg.append(", \"args\":[");
            loaded.forEach(arg -> {
                msg.append("  ");
                Json.appendString(msg, arg);
                msg.append(",\n");
            });
            msg.append("\"\"]"); // pure laziness around trailing comma
            msg.append("}},");
        });

        msg.append("\n{}]}"); // pure laziness around trailing comma

        try {
            final byte[] bytes = msg.toString().getBytes(StandardCharsets.UTF_8);

            final HttpURLConnection conn = (HttpURLConnection)
                    new URL("http://127.0.0.1:8000/api/v1/beacon").openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "text/plain charset=utf-8");
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
