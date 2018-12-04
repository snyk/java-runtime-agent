package demo;

import java.util.concurrent.atomic.AtomicLong;

public class CalledFromExecutor implements Runnable {
    private final AtomicLong work;

    CalledFromExecutor(AtomicLong work) {
        this.work = work;
    }

    @Override
    public void run() {
        // TODO: conditional branch here is always true, but a hack to ensure we watch the method
        if (null != work) {
            work.incrementAndGet();
        }
    }
}
