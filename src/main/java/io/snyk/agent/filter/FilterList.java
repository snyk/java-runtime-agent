package io.snyk.agent.filter;

import io.snyk.agent.logic.Config;
import io.snyk.agent.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class FilterList {
    /** className -> methodName -> group:artifact -> versions */
    private final Map<String, Map<String, Map<String, Set<String>>>> classMethodGaVersions;
    public final Instant generated;

    private FilterList(Map<String, Map<String, Map<String, Set<String>>>> classMethodGaVersions, Instant generated) {
        this.classMethodGaVersions = classMethodGaVersions;
        this.generated = generated;
    }

    public static FilterList loadBuiltInBolos(Log log) {
        try {
            log.debug("loading built-in filters from bundled snapshot");
            return loadBolosFrom(log,
                    loadResourceAsLines("/methods.bundled.properties"),
                    Instant.EPOCH);
        } catch (RuntimeException error) {
            log.warn("failed loading bundled snapshot, falling back to build-time snapshot");
            log.stackTrace(error);
        }

        return loadBolosFrom(log,
                loadResourceAsLines("/methods.properties"),
                Instant.EPOCH);
    }

    public static FilterList loadBolosFrom(Log log, Iterable<String> lines, Instant created) {
        final Map<String, Map<String, String>> configFile = new HashMap<>();

        Config.parsePropertiesFile(lines).forEach((key, value) -> {
            if (!key.startsWith("filter.")) {
                log.warn("invalid filter key: " + key);
                return;
            }

            final String[] parts = key.split("\\.", 3);
            if (3 != parts.length) {
                log.warn("invalid filter. key: " + key);
                return;
            }

            final String name = parts[1];
            final String command = parts[2];

            configFile.computeIfAbsent(name, (_name) -> new HashMap<>())
                    .put(command, value);
        });

        final Map<String, Map<String, Map<String, Set<String>>>> classMethodGaVersions = new HashMap<>();

        configFile.forEach((name, entry) -> {
            final String classAndMethod = entry.get("paths");
            final String mavenGroupArtifact = entry.get("artifact");
            final String versions = entry.get("version");

            if (null == versions || null == classAndMethod || null == mavenGroupArtifact) {
                throw new IllegalStateException("invalid config block: " + entry);
            }

            final String[] parts = classAndMethod.split("#", 2);
            if (2 != parts.length) {
                throw new IllegalStateException("invalid classAndMethod: " + classAndMethod);
            }

            final String internalClass = parts[0];
            final String method = parts[1];

            if (null == method || method.isEmpty()) {
                throw new IllegalStateException("method required");
            }

            classMethodGaVersions.computeIfAbsent(internalClass, (_class) -> new HashMap<>())
                    .computeIfAbsent(method, (_method) -> new HashMap<>())
                    .computeIfAbsent(mavenGroupArtifact, (_artifact) -> new HashSet<>())
                    .addAll(Arrays.asList(versions.split("\\s+")));
        });

        return new FilterList(classMethodGaVersions, created);
    }

    public static FilterList empty() {
        return new FilterList(Collections.emptyMap(), Instant.EPOCH);
    }

    private static List<String> loadResourceAsLines(String resourceName) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(FilterList.class.getResourceAsStream(
                resourceName)))) {
            return reader.lines().collect(Collectors.toList());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public int numberOfClasses() {
        return classMethodGaVersions.size();
    }

    public Set<String> knownClasses() {
        return classMethodGaVersions.keySet();
    }

    public Collection<String> methodsToInstrumentInClass(String className) {
        return classMethodGaVersions.getOrDefault(className, Collections.emptyMap()).keySet();
    }
}
