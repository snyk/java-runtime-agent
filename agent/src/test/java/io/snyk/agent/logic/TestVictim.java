package io.snyk.agent.logic;

import java.util.concurrent.Callable;

/**
 * Loaded (dynamically) by tests. Everything is public for a reason.
 */
public class TestVictim implements Callable<Number> {
    public int returnFive() {
        return 5;
    }

    public void emptyMethod() {
    }

    // I'm surprised this doesn't cause a linkage error of some kind.
    // Woo laziness.
    public native void bar();

    public <T> T localGeneric(T t) {
        return t;
    }

    public Callable<String> returnLambda() {
        return () -> "hello world";
    }

    int intField;

    int getIntField() {
        return intField;
    }

    void setIntField(int to) {
        intField = to;
    }

    String stringField;

    String getStringField() {
        return stringField;
    }

    void setStringField(String to) {
        stringField = to;
    }

    @Override
    public Number call() throws Exception {
        return 17;
    }
}
