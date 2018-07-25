package io.snyk.agent;

import java.util.*;

/**
 * An attempt at a fast counter. To be determined if it's actually fast.
 */
public class UseCounter {
    // @GuardedBy("this")
    private final List<String> lookup = new ArrayList<>();

    // assumptions: can only grow
    // reassignment @GuardedBy("this")
    private volatile int[] counters = new int[1024];

    public synchronized int add(String s) {
        final int id = lookup.size();
        lookup.add(s);

        if (id < counters.length) {
            return id;
        }

        // this is also not thread safe, will lose some(tm) updates during the copy
        counters = Arrays.copyOf(counters, counters.length * 2);

        return id;
    }

    public synchronized Set<String> drain() {
        int[] old = counters;
        counters = new int[counters.length];
        final HashSet<String> ret = new HashSet<>();
        for (int i = 0; i < old.length; i++) {
            if (0 != old[i]) {
                ret.add(lookup.get(i));
            }
        }
        return ret;
    }

    public void increment(int id) {
        // This is NOT thread safe.
        // We're trying to get some idea of how many times something was called.
        // TODO: benchmark against AtomicIntArray version
        // note: there's no AtomicBoolArray. In fact, in hotspot, there's no boolean arrays at all.
        counters[id] += 1;
    }
}

//counters.updateAndGet(current -> {
//final int current_length = current.length();
//
//if (id < current_length) {
//// someone has beaten us to it
//return current;
//}
//
//final AtomicIntegerArray newCounters = new AtomicIntegerArray(current_length * 2);
//for (int i = 0; i < current_length; i++) {
//newCounters.set(i, current.get(i));
//}
//
//return newCounters;
//});