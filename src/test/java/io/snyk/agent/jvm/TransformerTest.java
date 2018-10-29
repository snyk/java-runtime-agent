package io.snyk.agent.jvm;

import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;
import io.snyk.agent.logic.Config;
import io.snyk.agent.logic.DataTracker;
import io.snyk.agent.testutil.TestLogger;
import io.snyk.agent.util.Log;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransformerTest {
    @Test
    void testNoConfig() throws IOException {
        assertTrue(exampleChanges("Foo",
                ""));
    }

    @Test
    void testFilterPaths() throws IOException {
        assertFalse(exampleChanges("Foo",
                "filter.foo.paths = com/fake/**"));

        assertTrue(exampleChanges("Foo",
                "filter.bar.paths = io/snyk/**"
        ));

        assertTrue(exampleChanges("Foo",
                "filter.foo.paths = com/fake/**",
                "filter.bar.paths = io/snyk/**"
        ));
    }

    @Test
    void testFilterArtifact() throws IOException {
        assertTrue(exampleChanges("Foo",
                "filter.foo.artifact = maven:io.snyk.example:example",
                "filter.foo.paths = io/snyk/**"),
                "We know where the io.snyk classes are from, and it's correct, so rewrite");

        assertFalse(exampleChanges("Foo",
                "filter.foo.artifact = maven:com.fake:fake",
                "filter.foo.paths = io/snyk/**"),
                "We know where the io.snyk classes are from, and it's not com.fake:fake, so they are not rewritten");

        assertFalse(exampleChanges("Foo",
                "filter.foo.artifact = maven:io.snyk.example:example",
                "filter.foo.version = [,1.2.0)",
                "filter.foo.paths = io/snyk/**"),
                "We know where the io.snyk classes are from, they're newer than stated");

        assertTrue(exampleChanges("Foo",
                "filter.foo.artifact = maven:io.snyk.example:example",
                "filter.foo.version = [,1.3.0)",
                "filter.foo.paths = io/snyk/**"),
                "We know where the io.snyk classes are from, they're older than stated");
    }

    private boolean exampleChanges(String clazz, String... config) throws IOException {
        final URL srcJar = TransformerTest.class.getResource("/example-1.0-SNAPSHOT.jar");
        final URLClassLoader classLoader = new URLClassLoader(new URL[]{srcJar});
        final Log log = new TestLogger();
        final List<String> configItems = Lists.newArrayList(config);
        configItems.add("projectId=b2c2d38f-f147-4010-b92d-3dea94893d5b");
        final Transformer transformer = new Transformer(log,
                Config.fromLinesWithoutDefault(configItems),
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
