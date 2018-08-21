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
    // URL is a bad map key. A really bad map key.
    private final ConcurrentMap<URI, Set<String>> jarInfoMap = new ConcurrentHashMap<>();
    private final Log log;

    public ClassSource(Log log) {
        this.log = log;
    }

    public ExtraInfo findSourceInfo(final ClassLoader loader, final String className, final byte[] classfileBuffer) {
        try {
            final URL url = walkUpName(loader, className);
            if (null == url) {
                return new ExtraInfo(URI.create("unknown-class:" + className), Collections.emptySet());
            }

            final ExtraInfo info = sourceUri(url);
            info.crc = Crc32c.process(classfileBuffer);
            return info;
        } catch (Exception | ZipError e) {
            log.warn("couldn't process an input");
            e.printStackTrace();
        }
        return new ExtraInfo(URI.create("unknown-error:" + className), Collections.emptySet());
    }

    public Set<String> infoFor(URI sourceUri) {
        return jarInfoMap.getOrDefault(sourceUri, Collections.emptySet());
    }

    // So, this isn't super reliable.
    // Who knows what code is going to be in:
    // org/apache/maven/cli/configuration/SettingsXmlConfigurationProcessor$$FastClassByGuice$
    // Seems better than nothing, though? At least we'll find an approximate jar,
    // which might have the metadata for the thing that generated the class
    private URL walkUpName(final ClassLoader loader, final String className) {
        String shortenedName = className;
        URL url;

        while (true) {
            url = loader.getResource(shortenedName + ".class");

            if (null != url) {
                break;
            }

            final int pos = shortenedName.lastIndexOf('$');
            if (-1 == pos) {
                // so.. synthetics? Maybe?
                log.warn("couldn't load " + className);
                return null;
            }

            shortenedName = shortenedName.substring(0, pos);
        }

        return url;
    }

    public static class ExtraInfo {
        public final URI uri;
        public final Set<String> extra;
        int crc = 0;

        ExtraInfo(URI uri, Set<String> extra) {
            this.uri = uri;
            this.extra = extra;
        }

        public String toLocation() {
            return String.format("%08x:%s", crc, uri);
        }
    }

    /**
     * Process a URL down to our guess as to where the thing actually came from,
     * and maybe cache some information about that thing for later.
     */
    private ExtraInfo sourceUri(URL url) throws IOException, URISyntaxException {
        if ("jar".equals(url.getProtocol())) {

            final URLConnection conn = url.openConnection();

            if (conn instanceof JarURLConnection) {
                final JarURLConnection jarConn = (JarURLConnection) conn;
                final URI jarUri = jarConn.getJarFileURL().toURI();

                // we could have this as an actual cache; I like the idea of processing it asap,
                // just in case it vanishes before we try to report, or if the urlConnection is
                // broken or managed or something like that later
                final Set<String> extra = jarInfoMap.computeIfAbsent(jarUri, _jarUrl -> {
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

                return new ExtraInfo(jarUri, extra);
            }

            log.warn("looked like a jar file but it wasn't one when we opened it: " + url);
        }

        return new ExtraInfo(url.toURI(), Collections.emptySet());
    }

    /**
     * Walk through a jar file and find META-INF/maven/.../pom.properties, and turn
     * them back into locators ("org.apache.commons:commons-lang:1.0").
     */
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
                foundPoms.add("maven:" + groupId + ":" + artifactId + ":" + version);
            }
        }

        return foundPoms;
    }
}
