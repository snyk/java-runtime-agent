package io.snyk.agent.logic;

import io.snyk.agent.testutil.TestLogger;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ClassSourceTest {

    @Test
    void testNoManifests() {
        final TestLogger log = new TestLogger();
        // echo 'public class A {}' > A.java && javac A.java && jar Mcf no-manifest-a.jar A.class
        final ClassInfo.ExtraInfo info = extractExtraInfo("/no-manifest-a.jar", "A", log);
        assertEquals(Collections.emptySet(), info.extra, "no details expected for manifest-free jar");
        assertFalse(log.loggedAnyExceptions);
    }

    @Test
    void testEmptyManifest() {
        final TestLogger log = new TestLogger();
        // echo 'public class A {}' > A.java && javac A.java && jar cf empty-manifest-a.jar A.class
        final ClassInfo.ExtraInfo info = extractExtraInfo("/empty-manifest-a.jar", "A", log);
        assertEquals(Collections.emptySet(), info.extra, "no details expected for manifest-free jar");
        assertFalse(log.loggedAnyExceptions);
    }

    @Test
    void testPlainMaven() {
        final TestLogger log = new TestLogger();
        final ClassInfo.ExtraInfo info = extractExtraInfo("/example-1.0-SNAPSHOT.jar", "io/snyk/example/App",
                log);
        assertEquals(Collections.singleton("maven:io.snyk.example:example:1.2.3-SNAPSHOT"), info.extra);
        assertFalse(log.loggedAnyExceptions);
    }

    private ClassInfo.ExtraInfo extractExtraInfo(String resourceName,
                                                   String className, TestLogger log) {
        final URL srcJar = ClassSourceTest.class.getResource(resourceName);
        final URLClassLoader classLoader = new URLClassLoader(new URL[]{srcJar});
        final byte[] classfileBuffer = new byte[0];
        return new ClassSource(log).classInfo.findSourceInfo(classLoader, className, classfileBuffer);
    }
}
