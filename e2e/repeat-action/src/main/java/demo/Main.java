package demo;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

class Main {
    public static void main(String[] args) throws InterruptedException {
        final AtomicLong work = new AtomicLong();
        final Thread loop = new Thread(new LoopWithSleep(work));
        loop.start();

        final ScheduledExecutorService ex = Executors.newScheduledThreadPool(4);

        for (int delay = 0; delay < 4_000; delay += 500) {
            ex.schedule(() -> new ConstructedFromExecutor().foo(work), delay, TimeUnit.MILLISECONDS);
        }

        final CalledFromExecutor callee = new CalledFromExecutor(work);
        for (int delay = 0; delay < 4_000; delay += 500) {
            ex.schedule(callee, delay, TimeUnit.MILLISECONDS);
        }

        ex.shutdown();
        ex.awaitTermination(10, TimeUnit.SECONDS);
        loop.join(10_000);
        final long done = work.get();
        if (8 * 3 != done) {
            throw new IllegalStateException("done: " + done);
        }
    }
}
