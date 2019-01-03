package demo;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

class Main {
    // all JOBS workers generate EVENTS events, with DELAY_MS delay between them.
    // e.g. 20 events at 500ms delay is 10s.
    static final long EVENTS = 20;
    static final long DELAY_MS = 500;
    private static final long JOBS = 3;

    public static void main(String[] args) throws InterruptedException {
        final AtomicLong work = new AtomicLong();
        final Thread loop = new Thread(new LoopWithSleep(work));
        loop.start();

        final ScheduledExecutorService ex = Executors.newScheduledThreadPool(4);

        for (int event = 0; event < EVENTS; event += 1) {
            ex.schedule(() -> new ConstructedFromExecutor().foo(work), event * DELAY_MS, TimeUnit.MILLISECONDS);
        }

        final CalledFromExecutor callee = new CalledFromExecutor(work);
        for (int event = 0; event < EVENTS; event += 1) {
            ex.schedule(callee, event * DELAY_MS, TimeUnit.MILLISECONDS);
        }

        ex.shutdown();
        ex.awaitTermination(EVENTS * DELAY_MS, TimeUnit.MILLISECONDS);
        loop.join(EVENTS * DELAY_MS);
        final long done = work.get();
        if (EVENTS * JOBS != done) {
            throw new IllegalStateException("done: " + done);
        }
    }
}
