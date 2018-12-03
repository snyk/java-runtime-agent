package demo;

import java.util.concurrent.atomic.AtomicLong;

public class CalledFromExecutor implements Runnable {
    private final AtomicLong work;

    CalledFromExecutor(AtomicLong work) {
        this.work = work;
    }

    @Override
    public void run() {
        work.incrementAndGet();
    }
}
