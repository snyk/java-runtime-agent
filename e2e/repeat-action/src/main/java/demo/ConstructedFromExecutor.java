package demo;

import java.util.concurrent.atomic.AtomicLong;

class ConstructedFromExecutor {
    void foo(AtomicLong work) {
        work.incrementAndGet();
    }
}
