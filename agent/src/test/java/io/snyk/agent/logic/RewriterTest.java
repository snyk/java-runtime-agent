package io.snyk.agent.logic;


import com.google.common.collect.Sets;
import io.snyk.agent.testutil.DefinerLoader;
import io.snyk.agent.testutil.TestTracker;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RewriterTest {

    public static final String TEST_LOCATION = "13371337:tests";

    @Test
    public void smokeTest() throws Exception {
        final String name = TestVictim.class.getName();
        final byte[] bytes = new Rewriter(TestTracker.class, TestTracker.SEEN_SET::add, TEST_LOCATION)
                .rewrite(new ClassReader(name));
        final Class<?> clazz = new DefinerLoader().define(name, bytes);
        final Object instance = clazz.newInstance();
        assertEquals(5, clazz.getDeclaredMethod("returnFive").invoke(instance));
        assertEquals(Sets.newHashSet(
                "io/snyk/agent/logic/TestVictim:<init>:" + TEST_LOCATION,
                "io/snyk/agent/logic/TestVictim:returnFive:" + TEST_LOCATION),
                TestTracker.SEEN_SET.drain().methodEntries);
    }
}
