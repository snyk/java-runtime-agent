package io.snyk.agent.logic;

import io.snyk.agent.util.Crc32c;
import io.snyk.agent.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipError;

public class ClassSource {
    private final ConcurrentMap<URI, Set<String>> jarInfoMap = new ConcurrentHashMap<>();
    private final Log log;

    public ClassSource(Log log) {
        this.log = log;
    }

    public void observe(ClassLoader loader, String className, byte[] classfileBuffer) {
        try {
            final URL url = loader.getResource(className + ".class");
            if (null == url) {
                // so.. synthetics? Maybe?
                log.warn("couldn't load " + className);
                return;
            }

            int crc = Crc32c.process(classfileBuffer);

            sourceUri(url);
        } catch (Exception | ZipError e) {

            log.warn("couldn't process an input");
            e.printStackTrace();
        }
    }

    /**
     * Process a URL down to our guess as to where the thing actually came from,
     * and maybe cache some information about that thing for later.
     */
    private URI sourceUri(URL url) throws IOException, URISyntaxException {
        if ("jar".equals(url.getProtocol())) {

            final URLConnection conn = url.openConnection();

            if (conn instanceof JarURLConnection) {
                final JarURLConnection jarConn = (JarURLConnection) conn;
                final URI jarUri = jarConn.getJarFileURL().toURI();

                // we could have this as an actual cache; I like the idea of processing it asap,
                // just in case it vanishes before we try to report, or if the urlConnection is
                // broken or managed or something like that later
                jarInfoMap.computeIfAbsent(jarUri, _jarUrl -> {
                    // this function may be called multiple times in parallel;
                    // inefficient but not important
                    try {
                        return extractMavenLocators(jarConn.getJarFile());
                    } catch (IOException e) {
                        log.warn("looked like a jar file we couldn't process it: " + url);
                        e.printStackTrace();
                        return null;
                    }
                });

                return jarUri;
            }

            log.warn("looked like a jar file but it wasn't one when we opened it: " + url);
        }

        return url.toURI();
    }

    private Set<String> extractMavenLocators(JarFile jarFile) throws IOException {
        final Set<String> foundPoms = new HashSet<>();
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

            final String groupId = props.getProperty("groupId");
            final String artifactId = props.getProperty("artifactId");
            final String version = props.getProperty("version");

            if (null != groupId && null != artifactId && null != version) {
                foundPoms.add(groupId + ":" + artifactId + ":" + version);
            }
        }

        return foundPoms;
    }
}
