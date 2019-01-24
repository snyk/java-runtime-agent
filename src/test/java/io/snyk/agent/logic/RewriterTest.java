package io.snyk.agent.logic;

import com.google.common.collect.Sets;
import io.snyk.agent.testutil.DefinerLoader;
import io.snyk.agent.testutil.TestLogger;
import io.snyk.agent.testutil.TestTracker;
import io.snyk.agent.util.AsmUtil;
import io.snyk.asm.ClassReader;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class RewriterTest {

    private static final String TEST_LOCATION = "13371337:tests";

    @Test
    void smokeTest() throws Exception {
        final String name = TestVictim.class.getName();
        final byte[] bytes = rewrite(name);
        final Class<?> clazz = new DefinerLoader().define(name, bytes);
        final Object instance = clazz.newInstance();
        assertNotNull(clazz.getDeclaredMethod("returnLambda").invoke(instance));
        assertEquals(Sets.newHashSet(
                "io/snyk/agent/logic/TestVictim:returnLambda()Ljava/util/concurrent/Callable;:" + TEST_LOCATION),
                TestTracker.SEEN_SET.drain().methodEntries);
    }

    @Test
    void spotRewrite() throws Exception {
        final String name = TestVictim.class.getName();
        final String trackerName = TestTracker.class.getName().replace('.', '/');

        AsmUtil.parse(new ClassReader(name)).methods.forEach(method ->
                assertFalse(InstrumentationFilter.alreadyInstrumented(method, trackerName)));

        assertEquals(5, AsmUtil.parse(new ClassReader(rewrite(name))).methods
                .stream()
                .mapToInt(method -> InstrumentationFilter.alreadyInstrumented(method, trackerName) ? 1 : 0)
                .sum());
    }

    private byte[] rewrite(String name) throws IOException {
        return new Rewriter(TestTracker.class,
                TestTracker.SEEN_SET::add,
                TEST_LOCATION,
                ConfigTest.makeConfig(Arrays.asList(
                        "projectId=ab95b1fb-4fe0-497d-aba0-5a1d85db0827",
                        "trackClassLoading=true"),
                        Collections.singletonList("filter.foo.paths=**")
                ), new TestLogger()).rewrite(new ClassReader(name));
    }
}
