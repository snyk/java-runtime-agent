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

    // TODO: MVP storage. This is awful.
    static final ConcurrentHashMap<String, Object> SEEN_SET = new ConcurrentHashMap<>();

    // TODO: Awful. No, go back and read that again.

    // This is the hack a typical HashSet uses internally to adapt a map.
    private static final Object PRESENT = new Object();

    /**
     * Called by the instrumentation.
     */
    public static void registerCall(String site) {
        SEEN_SET.putIfAbsent(site, PRESENT);
    }

    public static void registerCallee(String site) {
        System.err.println("callee: " + site);
    }

}
