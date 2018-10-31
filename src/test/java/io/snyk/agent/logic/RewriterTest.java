package io.snyk.agent.logic;


import com.google.common.collect.Sets;
import io.snyk.agent.testutil.DefinerLoader;
import io.snyk.agent.testutil.TestTracker;
import io.snyk.asm.ClassReader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RewriterTest {

    private static final String TEST_LOCATION = "13371337:tests";

    @Test
    void smokeTest() throws Exception {
        final String name = TestVictim.class.getName();
        final byte[] bytes = new Rewriter(TestTracker.class,
                TestTracker.SEEN_SET::add,
                TEST_LOCATION,
                Config.fromLinesWithoutDefault(
                        "projectId=ab95b1fb-4fe0-497d-aba0-5a1d85db0827",
                        "trackClassLoading=true"
                )).rewrite(new ClassReader(name));
        final Class<?> clazz = new DefinerLoader().define(name, bytes);
        final Object instance = clazz.newInstance();
        assertNotNull(clazz.getDeclaredMethod("returnLambda").invoke(instance));
        assertEquals(Sets.newHashSet(
                "io/snyk/agent/logic/TestVictim:returnLambda()Ljava/util/concurrent/Callable;:" + TEST_LOCATION),
                TestTracker.SEEN_SET.drain().methodEntries);
    }
}
