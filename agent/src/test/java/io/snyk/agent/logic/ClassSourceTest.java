package io.snyk.agent.logic;

import io.snyk.agent.util.Log;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClassSourceTest {

    @Test
    void testNoManifests() {
        // echo 'public class A {}' > A.java && javac A.java && jar Mcf no-manifest-a.jar A.class
        final ClassSource.ExtraInfo info = extractExtraInfo("/no-manifest-a.jar");
        assertEquals(Collections.emptySet(), info.extra, "no details expected for manifest-free jar");
    }

    @Test
    void testEmptyManifest() {
        // echo 'public class A {}' > A.java && javac A.java && jar cf empty-manifest-a.jar A.class
        final ClassSource.ExtraInfo info = extractExtraInfo("/empty-manifest-a.jar");
        assertEquals(Collections.emptySet(), info.extra, "no details expected for manifest-free jar");
    }

    @Test
    void testPlainMaven() {
        final ClassSource.ExtraInfo info = extractExtraInfo("/example-1.0-SNAPSHOT.jar", "io/snyk/example/App");
        assertEquals(Collections.singleton("maven:io.snyk.example:example:1.2.3-SNAPSHOT"),
                info.extra,
                "no details expected for manifest-free jar");
    }

    private ClassSource.ExtraInfo extractExtraInfo(String resourceName) {
        return extractExtraInfo(resourceName, "A");
    }

    private ClassSource.ExtraInfo extractExtraInfo(String resourceName,
                                                   String className) {
        final URL srcJar = ClassSourceTest.class.getResource(resourceName);
        final URLClassLoader classLoader = new URLClassLoader(new URL[]{srcJar});
        return new ClassSource(new Log()).findSourceInfo(classLoader, className, new byte[0]);
    }
}
