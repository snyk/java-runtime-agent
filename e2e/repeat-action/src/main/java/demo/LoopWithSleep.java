package demo;

import java.util.concurrent.atomic.AtomicLong;

public class LoopWithSleep implements Runnable {
    private final AtomicLong work;

    LoopWithSleep(AtomicLong work) {
        this.work = work;
    }

    @Override
    public void run() {
        for (int i = 0; i < 8; ++i) {
            work.incrementAndGet();
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                return;
            }
        }
    }
}
