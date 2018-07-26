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

        final AtomicIntegerArray newCounters = new AtomicIntegerArray(currentLength * 2);

        // this is also not thread safe, will lose some(tm) updates during the copy
        for (int i = 0; i < currentLength; i++) {
            newCounters.set(i, counters.get(i));
        }
        counters = newCounters;

        return id;
    }

    public synchronized Drain drain() {
        final Drain ret = new Drain();

        // this is not truly loss-free, but I believe it would be in practice?
        // the reassignment happens-before we observe any of the values in our backup
        final AtomicIntegerArray old = counters;
        counters = new AtomicIntegerArray(counters.length());
        for (int i = 0; i < old.length(); i++) {
            if (0 != old.get(i)) {
                ret.methodEntries.add(lookup.get(i));
            }
        }

        loadClassCalls.forEach((id, names) ->
                ret.loadClasses.put(lookup.get(id), names));
        loadClassCalls.clear();

        return ret;
    }

    public void increment(int id) {
        counters.getAndIncrement(id);
    }

    public synchronized void loadClassCall(int id, String arg) {
        loadClassCalls.computeIfAbsent(id, underscore -> new HashSet<>()).add(arg);
    }
}
