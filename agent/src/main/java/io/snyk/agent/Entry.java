package io.snyk.agent;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.reflect.InvocationTargetException;

/**
 * The entry point for the agent. Load and install our plugins.
 */
public class Entry {
    private static final boolean LOAD = false;

    public static void premain(
            String agentArguments,
            Instrumentation instrumentation) throws UnmodifiableClassException {
        instrumentation.addTransformer(new Transformer(), true);

        if (LOAD) {
            startTracker();
        }
    }

    // TODO: seeing duplicate class problems with this; and currently aren't using it
    private static void startTracker() {
        try {
            Object tracker = ClassLoader.getSystemClassLoader().loadClass(Tracker.class.getName()).newInstance();
            tracker.getClass().getDeclaredMethod("start").invoke(tracker);
        } catch (InstantiationException |
                IllegalAccessException |
                ClassNotFoundException |
                InvocationTargetException |
                NoSuchMethodException e) {
            throw new IllegalStateException("Couldn't find ourselves", e);
        }
    }
}
