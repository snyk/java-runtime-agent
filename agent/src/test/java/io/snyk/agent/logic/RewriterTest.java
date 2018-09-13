package io.snyk.agent.logic;


import com.google.common.collect.Sets;
import io.snyk.agent.testutil.DefinerLoader;
import io.snyk.agent.testutil.TestTracker;
import org.junit.jupiter.api.Test;
import io.snyk.asm.ClassReader;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RewriterTest {

    private static final String TEST_LOCATION = "13371337:tests";

    @Test
    void smokeTest() throws Exception {
        final String name = TestVictim.class.getName();
        final byte[] bytes = new Rewriter(TestTracker.class, TestTracker.SEEN_SET::add, TEST_LOCATION, true)
                .rewrite(new ClassReader(name));
        final Class<?> clazz = new DefinerLoader().define(name, bytes);
        final Object instance = clazz.newInstance();
        assertEquals(5, clazz.getDeclaredMethod("returnFive").invoke(instance));
        assertEquals(Sets.newHashSet(
                "io/snyk/agent/logic/TestVictim:<init>()V:" + TEST_LOCATION,
                "io/snyk/agent/logic/TestVictim:returnFive()I:" + TEST_LOCATION),
                TestTracker.SEEN_SET.drain().methodEntries);
    }
}
