package io.snyk.agent;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
    private static final ConcurrentHashMap<String, Object> SEEN_SET = new ConcurrentHashMap<>();

    // TODO: Awful. No, go back and read that again.

    // This is the hack a typical HashSet uses internally to adapt a map.
    private static final Object PRESENT = new Object();

    /**
     * Called by the instrumentation.
     */
    public static void registerCall(String site) {
        System.err.println("registered: " + site);
        SEEN_SET.put(site, PRESENT);
    }

    private static class Explainer implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    work();
                } catch (Throwable t) {
                    System.err.println("agent issue");
                    t.printStackTrace();
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }

        void work() {
            System.err.println("I've seen " + SEEN_SET.size() + " objects");
        }
    }
}
