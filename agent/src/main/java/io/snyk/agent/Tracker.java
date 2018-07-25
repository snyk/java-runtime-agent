package io.snyk.agent;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Track work done by callers.
 * <p>
 * Note that this is run in a different classloader to the agent.
 * TODO: Unclear how careful we need to be about ensuring there's
 * only one of us, and instantiating workers only in the right place.
 */
public class Tracker {
    static {
        System.err.println("Tracker being loaded by:");
        for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
            System.err.println(" * " + ste);
        }

        Thread worker = new Thread(new Explainer());
        worker.setDaemon(true);
        worker.setName("snyk-agent");
        worker.start();
    }

    static final UseCounter SEEN_SET = new UseCounter();

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
