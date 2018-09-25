package io.snyk.agent.util;

import java.util.*;
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

    // @GuardedBy("this")
    private HashMap<Integer, HashSet<String>> loadClassCalls = new HashMap<>();

    public static class Drain {
        public final Set<String> methodEntries = new HashSet<>();
        public final HashMap<String, HashSet<String>> loadClasses = new HashMap<>();
    }

    public synchronized int add(String s) {
        final int id = lookup.size();
        lookup.add(s);

        final int currentLength = counters.length();
        if (id < currentLength) {
            return id;
        }

        final AtomicIntegerArray old = this.counters;
        this.counters = new AtomicIntegerArray(currentLength * 2);

        for (int i = 0; i < currentLength; i++) {
            if (0 != old.get(i)) {
                this.counters.set(i, 1);
            }
        }

        return id;
    }

    public synchronized Drain drain() {
        final Drain ret = new Drain();

        // I'm reasonably confident this is fine, we're using volatile access to read the values out,
        // and nobody else is assigning to `counters` while we're running. Also, it's O(n) and we're
        // not doing any real work (beyond synchronisation overheads) anywhere; log(n) allocation (for the hashmap),
        // no copying, etc.
        final AtomicIntegerArray old = counters;
        counters = new AtomicIntegerArray(counters.length());
        for (int i = 0; i < old.length(); i++) {
            if (0 != old.get(i)) {
                // lookup.get() is "volatile read" here.
                ret.methodEntries.add(lookup.get(i));
            }
        }

        loadClassCalls.forEach((id, names) ->
                ret.loadClasses.put(lookup.get(id), names));
        loadClassCalls.clear();

        return ret;
    }

    public void increment(int id) {
        counters.lazySet(id, 1);
    }

    public synchronized void loadClassCall(int id, String arg) {
        loadClassCalls.computeIfAbsent(id, underscore -> new HashSet<>()).add(arg);
    }
}
