package demo;

import java.util.concurrent.atomic.AtomicLong;

public class LoopWithSleep implements Runnable {
    private final AtomicLong work;

    LoopWithSleep(AtomicLong work) {
        this.work = work;
    }

    @Override
    public void run() {
        for (int i = 0; i < Main.EVENTS; ++i) {
            foo();
            try {
                Thread.sleep(Main.DELAY_MS);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    private void foo() {
        // TODO: conditional branch here is always true, but a hack to ensure we watch the method
        if (null != work) {
            work.incrementAndGet();
        }
    }
}
