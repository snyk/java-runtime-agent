package io.snyk.agent.jvm;

import io.snyk.agent.logic.ReportingWorker;
import io.snyk.agent.util.UseCounter;

/**
 * Track work done by callers.
 * <p>
 * Note that this is run in a different classloader to the agent.
 * TODO: Unclear how careful we need to be about ensuring there's
 * only one of us, and instantiating workers only in the right place.
 */
public class LandingZone {
    static {
        System.err.println("LandingZone being loaded by:");
        for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
            System.err.println(" * " + ste);
        }

        Thread worker = new Thread(new ReportingWorker());
        worker.setDaemon(true);
        worker.setName("snyk-agent");
        worker.start();
    }

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
    public static void registerLoadClass(String arg, int id) {
        SEEN_SET.loadClassCall(id, arg);
    }
}
