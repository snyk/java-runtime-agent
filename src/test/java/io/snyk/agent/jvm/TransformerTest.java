package io.snyk.agent.jvm;

import com.google.common.io.ByteStreams;
import io.snyk.agent.logic.ConfigTest;
import io.snyk.agent.logic.DataTracker;
import io.snyk.agent.testutil.TestLogger;
import io.snyk.agent.util.Log;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransformerTest {
    @Test
    void testFilterPaths() throws IOException {
        assertFalse(exampleChanges("Foo",
                "filter.foo.paths = com/fake/Nope#nah",
                "filter.foo.artifact = maven:nah:nah",
                "filter.foo.version = [4,5)"));

        assertTrue(exampleChanges("Foo",
                "filter.bar.paths = io/snyk/example/Foo#bar",
                "filter.bar.artifact = maven:nah:nah",
                "filter.bar.version = [4,5)"
        ));

        assertTrue(exampleChanges("Foo",
                "filter.foo.paths = com/fake/Nope#nah",
                "filter.bar.paths = io/snyk/example/Foo#bar",
                "filter.foo.artifact = maven:nah:nah",
                "filter.foo.version = [4,5)",
                "filter.bar.artifact = maven:nah:nah",
                "filter.bar.version = [4,5)"
        ));
    }

    private boolean exampleChanges(String clazz, String... filters) throws IOException {
        final URL srcJar = TransformerTest.class.getResource("/example-1.0-SNAPSHOT.jar");
        final URLClassLoader classLoader = new URLClassLoader(new URL[]{srcJar});
        final Log log = new TestLogger();
        final Transformer transformer = new Transformer(log,
                ConfigTest.makeConfig(
                        Collections.singleton("projectId=b2c2d38f-f147-4010-b92d-3dea94893d5b"),
                        Arrays.asList(filters)),
                new DataTracker(log));
        final byte[] originalBytes = ByteStreams.toByteArray(classLoader.getResourceAsStream(
                "io/snyk/example/" + clazz + ".class"));
        final byte[] newBytes = transformer.transform(classLoader, null, null, null, originalBytes);

        if (null == newBytes) {
            return false;
        }

        return !Arrays.equals(originalBytes, newBytes);
    }
}
