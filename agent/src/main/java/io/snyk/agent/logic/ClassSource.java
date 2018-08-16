package io.snyk.agent.logic;

import io.snyk.agent.util.Crc32c;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.zip.ZipError;

public class ClassSource {
    private final ConcurrentMap<String, String> jarInfoMap = new ConcurrentHashMap<>();

    public void observe(ClassLoader loader, String className, byte[] classfileBuffer) {
        try {
            final URL url = loader.getResource(className + ".class");
            if (null == url) {
                // so.. synthetics? Maybe?
                System.err.println("couldn't load " + className);
                return;
            }

            int crc = Crc32c.process(classfileBuffer);

            String found = locator(url);
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
                            System.err.println(jarUrl + ": " + extractPoms(jarConn.getJarFile()));
                        } catch (IOException e) {
                            throw new IllegalStateException(e);
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

    private Set<Map<String, String>> extractPoms(JarFile jarFile) throws IOException {
        final Set<Map<String, String>> foundPoms = new HashSet<>();
        final Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            final JarEntry entry = entries.nextElement();
            if (entry.isDirectory()) {
                continue;
            }

            final String name = entry.getName();
            if (!name.startsWith("META-INF/maven/")) {
                continue;
            }

            if (!name.endsWith("/pom.properties")) {
                continue;
            }

            final Properties props = new Properties();
            try (InputStream is = jarFile.getInputStream(entry)) {
                props.load(is);
            }

            final Map<String, String> asMap = props.entrySet()
                    .stream()
                    .collect(Collectors.toMap(t -> String.valueOf(t.getKey()), t -> String.valueOf(t.getValue())));

            foundPoms.add(asMap);
        }

        return foundPoms;
    }

}
