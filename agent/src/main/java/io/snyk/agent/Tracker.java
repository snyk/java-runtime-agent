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
    // TODO: MVP storage. This is awful.
    private static final ConcurrentHashMap<String, Object> SEEN_SET = new ConcurrentHashMap<>();

    // TODO: Awful. No, go back and read that again.

    // This is the hack a typical HashSet uses internally to adapt a map.
    private static final Object PRESENT = new Object();

    private static final ScheduledExecutorService SCHEDULE = Executors.newScheduledThreadPool(1);

    /**
     * Called by reflection from the agent. Which context will we be in?
     * TODO: Context context context.
     */
    public void start() {
//        SCHEDULE.scheduleAtFixedRate(new Explainer(), 0, 1, TimeUnit.SECONDS);
    }

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
            // TODO: VM doesn't seem to want to shut down even with this.
            // TODO: Maybe it's going down too fast?
            Thread.currentThread().setDaemon(true);
            System.err.println("I've seen " + SEEN_SET.size() + " objects");
        }
    }
}
