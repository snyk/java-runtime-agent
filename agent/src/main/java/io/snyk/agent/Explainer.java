package io.snyk.agent;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

class Explainer implements Runnable {
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
        // TODO: Worst attempt at concurrency ever.
        // TODO: We will miss "some" calls that occur while we're iterating.
        // TODO: for hourly sampling, this isn't too significant
        // TODO: Chris promises us he remembers there was a time when ConcurrentHashMap
        // TODO: could atomically drain, but it is not to be. Must have been RUST.

        final StringBuilder msg = new StringBuilder();
        for (String loc : Tracker.SEEN_SET.keySet()) {
            msg.append(loc);
            msg.append('\n');
        }
        Tracker.SEEN_SET.clear();

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
