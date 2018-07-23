package io.snyk.agent;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;

/**
 * The entry point for the agent. Load and install our plugins.
 */
public class Entry {
    public static void premain(String agentArguments, Instrumentation instrumentation) throws UnmodifiableClassException {
        instrumentation.addTransformer(new Transformer(), true);
        try {
            ClassLoader.getSystemClassLoader().loadClass(Transformer.class.getName());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Couldn't find ourselves");
        }
    }
}
