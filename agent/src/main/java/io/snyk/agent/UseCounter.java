package io.snyk.agent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicIntegerArray;

/**
 * An attempt at a fast counter. To be determined if it's actually fast.
 */
public class UseCounter {
    // @GuardedBy("this")
    private final List<String> lookup = new ArrayList<>();

    // assumptions: can only grow
    // reassignment @GuardedBy("this")
    private volatile AtomicIntegerArray counters = new AtomicIntegerArray(1024);

    public synchronized int add(String s) {
        final int id = lookup.size();
        lookup.add(s);

        final int currentLength = counters.length();
        if (id < currentLength) {
            return id;
        }

        final AtomicIntegerArray newCounters = new AtomicIntegerArray(currentLength * 2);

        // this is also not thread safe, will lose some(tm) updates during the copy
        for (int i = 0; i < currentLength; i++) {
            newCounters.set(i, counters.get(i));
        }
        counters = newCounters;

        return id;
    }

    public synchronized Set<String> drain() {
        // this is not truly loss-free, but I believe it would be in practice?
        // the reassignment happens-before we observe any of the values in our backup
        final AtomicIntegerArray old = counters;
        counters = new AtomicIntegerArray(counters.length());
        final HashSet<String> ret = new HashSet<>();
        for (int i = 0; i < old.length(); i++) {
            if (0 != old.get(i)) {
                ret.add(lookup.get(i));
            }
        }
        return ret;
    }

    public void increment(int id) {
        counters.getAndIncrement(id);
    }
}
