package io.snyk.agent.testutil;

import io.snyk.agent.util.UseCounter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestTracker {
    private static final Logger logger = LoggerFactory.getLogger(TestTracker.class);

    public static final UseCounter SEEN_SET = new UseCounter();

    /**
     * Called by the instrumentation.
     */
    public static void registerMethodEntry(int id) {
        SEEN_SET.increment(id);
    }

    /**
     * Called by the instrumentation.
     */
    public static void registerLoadClass(String arg, String site) {
        logger.debug("callee: " + site + ":" + arg);
    }

}
