package io.snyk.agent.testutil;

import io.snyk.agent.util.UseCounter;

public class TestTracker {
    public static final UseCounter SEEN_SET = new UseCounter();

    /**
     * Called by the instrumentation.
     */
    public static void registerCall(int id) {
        SEEN_SET.increment(id);
    }

    /**
     * Called by the instrumentation.
     */
    public static void registerCallee(String arg, String site) {
        System.err.println("callee: " + site + ":" + arg);
    }

}
