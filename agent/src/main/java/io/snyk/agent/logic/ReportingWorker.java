package io.snyk.agent.logic;

import io.snyk.agent.jvm.LandingZone;
import io.snyk.agent.util.UseCounter;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ReportingWorker implements Runnable {
    @Override
    public void run() {
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

    void work() {
        final StringBuilder msg = new StringBuilder();
        final UseCounter.Drain drain = LandingZone.SEEN_SET.drain();
        for (String loc : drain.methodEntries) {
            msg.append(loc);
            msg.append('\n');
        }

        drain.loadClasses.forEach((caller, loaded) -> {
            msg.append(caller);
            loaded.forEach(arg -> {
                msg.append(' ');
                msg.append(arg);
            });
        });

        try {
            final HttpURLConnection conn = (HttpURLConnection)new URL("http://127.0.0.1:5000/dump").openConnection();
            conn.setRequestMethod("PUT");
            conn.setRequestProperty("Content-Type", "text/plain charest=utf-8");
            conn.setDoOutput(true);
            try (final OutputStream body = conn.getOutputStream()) {
                body.write(msg.toString().getBytes(StandardCharsets.UTF_8));
            }
            conn.getInputStream().close();
            conn.disconnect();
        } catch (IOException e) {
            System.err.println("snyk explainer");
            e.printStackTrace();
        }
    }
}
