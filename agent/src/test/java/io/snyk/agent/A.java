package io.snyk.agent;

import java.util.concurrent.Callable;

/**
 * Loaded (dynamically) by tests. Everything is public for a reason.
 */
public class A {
    public int returnFive() {
        return 5;
    }

    public void emptyMethod() {
    }

    // I'm surprised this doesn't cause a linkage error of some kind.
    // Woo laziness.
    public native void bar();

    public <T> T synthetic(T t) {
        return t;
    }

    public Callable<String> returnLambda() {
        return () -> "hello world";
    }
}
