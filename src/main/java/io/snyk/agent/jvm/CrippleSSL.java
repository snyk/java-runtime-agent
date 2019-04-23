package io.snyk.agent.jvm;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

public class CrippleSSL {
    private static final X509TrustManager IGNORE_EVERYTHING_TRUST_MANAGER = new X509TrustManager() {

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    };

    /**
     * Disable CA and hostname verification on a connection, if it's an HTTPS connection.
     */
    public static void cripple(URLConnection conn) {
        if (!(conn instanceof HttpsURLConnection)) {
            return;
        }

        final HttpsURLConnection sslConn = (HttpsURLConnection) conn;

        try {
            final SSLContext crippledContext = SSLContext.getInstance("SSL");
            crippledContext.init(null, new TrustManager[]{IGNORE_EVERYTHING_TRUST_MANAGER}, new SecureRandom());
            sslConn.setSSLSocketFactory(crippledContext.getSocketFactory());
            sslConn.setHostnameVerifier((_anyName, _anySession) -> true);
        } catch (KeyManagementException | NoSuchAlgorithmException e) {
            throw new IllegalStateException("unable to disable ssl validation", e);
        }
    }
}
