package io.snyk.agent.jvm;

import com.google.common.io.ByteStreams;
import io.snyk.agent.logic.ClassSource;
import io.snyk.agent.logic.Config;
import io.snyk.agent.util.Log;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;

class TransformerTest {
    @Test
    void testTransform() throws IOException {
        final URL srcJar = TransformerTest.class.getResource("/example-1.0-SNAPSHOT.jar");
        final URLClassLoader classLoader = new URLClassLoader(new URL[]{srcJar});
        final Log logger = new Log();
        final Transformer transformer = new Transformer(logger, Config.fromLines(Arrays.asList(
                "",
                ""
        )), new ClassSource(logger));
        final byte[] originalBytes = ByteStreams.toByteArray(classLoader.getResourceAsStream("io/snyk/example/Foo.class"));
        final byte[] newBytes = transformer.transform(classLoader, null, null, null, originalBytes);

        assertFalse(Arrays.equals(originalBytes, newBytes));
    }
}
