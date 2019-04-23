package io.snyk.agent.jvm;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import static org.junit.jupiter.api.Assertions.*;

class CrippleSSLTest {
    @Test
    void againstBadSslCom() throws IOException {
        final URL testServer = new URL("https://self-signed.badssl.com/");
        final URLConnection failingConn = testServer.openConnection();
        try {
            failingConn.connect();
            failingConn.getInputStream().close();
            failingConn.getOutputStream().close();
            fail("default validation not working");
        } catch (IOException ignored) {
        }

        final URLConnection workingConn = testServer.openConnection();
        CrippleSSL.cripple(workingConn);
        workingConn.connect();
        final InputStream is = workingConn.getInputStream();
        assertNotEquals(-1, is.read());
        is.close();
    }
}
