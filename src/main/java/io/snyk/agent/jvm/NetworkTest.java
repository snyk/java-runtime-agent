package io.snyk.agent.jvm;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

class NetworkTest {
    static void run(String arg) throws IOException {
        log("Connection test");
        final URL url = new URL(arg);
        log("Testing connection to: " + url);
        log("Protocol: " + url.getProtocol());
        final URLConnection conn = url.openConnection();
        log("Connection ready.");
        conn.connect();
        log("Connection initiated.");
        boolean shouldRead = true;
        if (conn instanceof HttpURLConnection) {
            final HttpURLConnection http = (HttpURLConnection) conn;
            final int responseCode = http.getResponseCode();
            log("Response code: " + responseCode);

            shouldRead = (responseCode / 100) == 2;
        }

        if (shouldRead) {
            log("Attempting to read.");
            byte[] buf = new byte[8 * 1024];
            int len = conn.getInputStream().read(buf);
            log("Response prefix: " + new String(buf, 0, len, StandardCharsets.ISO_8859_1));
        } else {
            log("Read not attempted.");
        }

        log("Complete.");
    }

    private static void log(String message) {
        System.out.println(DateTimeFormatter.ISO_INSTANT.format(Instant.now()) + ": " + message);
    }
}
