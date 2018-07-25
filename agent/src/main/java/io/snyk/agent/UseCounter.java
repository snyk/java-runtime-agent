package io.snyk.agent;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * An attempt at a fast counter. To be determined if it's actually fast.
 */
public class UseCounter {
    // @GuardedBy("this")
    private final List<String> lookup = new ArrayList<>();

    // assumptions: can only grow
    // reassignment @GuardedBy("this")
    private final AtomicReference<int[]> counters = new AtomicReference<>(new int[1024]);

    public synchronized int add(String s) {
        final int id = lookup.size();
        lookup.add(s);

        // double-checked! Pretty sure this will have real value here
        if (id < counters.get().length) {
            return id;
        }

        // this is also not thread safe, updates while this is running will be lost.
        // this is really just a convenience method; we can never lose the race.
        counters.updateAndGet(current -> Arrays.copyOf(current, current.length * 2));

        return id;
    }

    public synchronized Set<String> drain() {
        int[] old = counters.getAndUpdate(current -> new int[current.length]);
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
        counters.get()[id] += 1;
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