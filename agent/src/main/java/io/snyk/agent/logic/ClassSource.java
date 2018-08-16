package io.snyk.agent.logic;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.zip.ZipError;

public class ClassSource {
    private final ConcurrentMap<String, String> jarInfoMap = new ConcurrentHashMap<>();

    public void observe(ClassLoader loader, String className) {
        try {
            final URL url = loader.getResource(className + ".class");
            if (null == url) {
                // so.. synthetics? Maybe?
                System.err.println("couldn't load " + className);
                return;
            }

            locator(url);
        } catch (Exception | ZipError e) {
            e.printStackTrace();
        }
    }

    private String locator(URL url) throws IOException {
        switch (url.getProtocol()) {
            case "jar": {
                final URLConnection conn = url.openConnection();
                if (conn instanceof JarURLConnection) {
                    final JarURLConnection jarConn = (JarURLConnection) conn;
                    return jarInfoMap.computeIfAbsent(jarConn.getJarFileURL().toString(), jarUrl -> {
                        try {
                            System.err.println(jarUrl);
                            System.err.println("manifest: " + jarConn.getManifest().getEntries());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        return "";
                    });
                } else {
                    System.err.println("snyk-agent: not a jar file: " + url);
                }
            }
        }

        return url.toString();
    }
}
